package finance.project.api.runs;

import finance.project.api.entities.run.RunErrorEntity;
import finance.project.api.entities.run.RunExecutionEntity;
import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.entities.run.RunRequestEntity;
import finance.project.api.observability.RunMetrics;
import java.util.concurrent.Executor;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import finance.project.api.repositories.RunRequestRepository;
import java.util.Optional;
import finance.project.api.repositories.RunExecutionRepository;
import finance.project.api.repositories.RunErrorRepository;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultRunDispatcher implements RunDispatcher {
    private static final Logger log = LoggerFactory.getLogger(DefaultRunDispatcher.class);

    private static final int MAX_ATTEMPTS = 3;

    private final Executor runDispatchExecutor;
    private final RunRequestRepository runRequestRepository;
    private final RunExecutionRepository runExecutionRepository;
    private final RunErrorRepository runErrorRepository;
    private final PythonAsyncClient pythonAsyncClient;
    private final DispatchBackoff dispatchBackoff;
    private final Clock clock;
    private final RunMetrics runMetrics;

    public DefaultRunDispatcher(@Qualifier("runDispatchExecutor") Executor runDispatchExecutor,
                                RunRequestRepository runRequestRepository,
                                RunExecutionRepository runExecutionRepository,
                                RunErrorRepository runErrorRepository,
                                PythonAsyncClient pythonAsyncClient,
                                DispatchBackoff dispatchBackoff,
                                Clock clock,
                                RunMetrics runMetrics) {
        this.runDispatchExecutor = runDispatchExecutor;
        this.runRequestRepository = runRequestRepository;
        this.runExecutionRepository = runExecutionRepository;
        this.runErrorRepository = runErrorRepository;
        this.pythonAsyncClient = pythonAsyncClient;
        this.dispatchBackoff = dispatchBackoff;
        this.clock = clock;
        this.runMetrics = runMetrics;
    }

    @Override
    public void enqueue(String requestId) {
        String normalizedId = normalizeRequestId(requestId);
        if (normalizedId == null) {
            return;
        }
        runDispatchExecutor.execute(() -> dispatchWithRetries(normalizedId));
    }

    void dispatchWithRetries(String requestId) {
        MDC.put("requestId", requestId);
        try {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                long attemptStartNs = System.nanoTime();
                Optional<RunRequestEntity> optional = runRequestRepository.findByRequestId(requestId);
                if (optional.isEmpty()) {
                    log.warn("[Dispatch] requestId={} attempt={} status=FAIL error=missing_run", requestId, attempt);
                    return;
                }

                RunRequestEntity runRequest = optional.get();
                MDC.put("specType", runRequest.getSpecType());
                RunExecutionEntity execution = loadOrCreateExecution(requestId, runRequest.getStatus());
                if (execution.getPythonJobId() != null && !execution.getPythonJobId().isBlank()) {
                    return;
                }
                if (runRequest.getStatus() != RunLifecycleStatus.PENDING && runRequest.getStatus() != RunLifecycleStatus.DISPATCH_FAILED) {
                    return;
                }

                updateAttempt(runRequest, execution, attempt, null);
                try {
                    String jobId = pythonAsyncClient.submitAsync(requestId, runRequest.getSpecGenerated());
                    markRunning(runRequest, execution, jobId);
                    long latencyMs = (System.nanoTime() - attemptStartNs) / 1_000_000;
                    runMetrics.recordDispatchLatencyMillis(latencyMs);
                    MDC.put("status", "OK");
                    MDC.put("latency_ms", String.valueOf(latencyMs));
                    log.info("dispatch_ok");
                    return;
                } catch (PythonDispatchException ex) {
                    String message = ex.getMessage();
                    updateAttempt(runRequest, execution, attempt, message);
                    boolean shouldRetry = ex.isRetryable() && attempt < MAX_ATTEMPTS;
                    long latencyMs = (System.nanoTime() - attemptStartNs) / 1_000_000;
                    runMetrics.recordDispatchLatencyMillis(latencyMs);
                    MDC.put("status", "FAIL");
                    MDC.put("latency_ms", String.valueOf(latencyMs));
                    log.warn("dispatch_fail retryable={} error={}", ex.isRetryable(), message);
                    if (!shouldRetry) {
                        markDispatchFailed(runRequest, execution, message, ex.getHttpStatus());
                        runMetrics.incrementDispatchFailed();
                        runMetrics.incrementRunsFailed("dispatch");
                        return;
                    }
                    dispatchBackoff.backoff(attempt);
                } catch (Exception ex) {
                    String message = ex.getMessage();
                    updateAttempt(runRequest, execution, attempt, message);
                    long latencyMs = (System.nanoTime() - attemptStartNs) / 1_000_000;
                    runMetrics.recordDispatchLatencyMillis(latencyMs);
                    MDC.put("status", "FAIL");
                    MDC.put("latency_ms", String.valueOf(latencyMs));
                    log.warn("dispatch_fail error={}", message);
                    markDispatchFailed(runRequest, execution, message, null);
                    runMetrics.incrementDispatchFailed();
                    runMetrics.incrementRunsFailed("dispatch");
                    return;
                } finally {
                    MDC.remove("latency_ms");
                    MDC.remove("status");
                }
            }
        } finally {
            MDC.remove("specType");
            MDC.remove("requestId");
        }
    }

    @Transactional
    void updateAttempt(RunRequestEntity runRequest, RunExecutionEntity execution, int attempt, String lastError) {
        execution.setDispatchAttempts(attempt);
        execution.setLastDispatchError(lastError);
        execution.setStatus(runRequest.getStatus());
        runExecutionRepository.save(execution);
    }

    @Transactional
    void markRunning(RunRequestEntity runRequest, RunExecutionEntity execution, String pythonJobId) {
        Instant now = clock.instant();
        execution.setPythonJobId(pythonJobId);
        execution.setStatus(RunLifecycleStatus.RUNNING);
        execution.setStartedAt(now);
        execution.setLastDispatchError(null);
        runExecutionRepository.save(execution);

        runRequest.setStatus(RunLifecycleStatus.RUNNING);
        runRequestRepository.save(runRequest);
    }

    @Transactional
    void markDispatchFailed(RunRequestEntity runRequest, RunExecutionEntity execution, String message, Integer httpStatus) {
        execution.setStatus(RunLifecycleStatus.DISPATCH_FAILED);
        execution.setLastDispatchError(message);
        runExecutionRepository.save(execution);

        runRequest.setStatus(RunLifecycleStatus.DISPATCH_FAILED);
        runRequestRepository.save(runRequest);

        runErrorRepository.save(RunErrorEntity.builder()
                .requestId(runRequest.getRequestId())
                .source(RunErrorEntity.Source.DISPATCH)
                .code(httpStatus == null ? null : "HTTP_" + httpStatus)
                .message(message)
                .build());
    }

    private RunExecutionEntity loadOrCreateExecution(String requestId, RunLifecycleStatus status) {
        return runExecutionRepository.findByRequestId(requestId).orElseGet(() -> runExecutionRepository.save(
                RunExecutionEntity.builder()
                        .requestId(requestId)
                        .dispatchAttempts(0)
                        .status(status == null ? RunLifecycleStatus.PENDING : status)
                        .build()
        ));
    }

    private static String normalizeRequestId(String requestId) {
        if (requestId == null) {
            return null;
        }
        String trimmed = requestId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
