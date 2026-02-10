package finance.project.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.run.RunExecutionEntity;
import finance.project.api.entities.run.RunErrorEntity;
import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.entities.run.RunRequestEntity;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.RunSubmitResponse;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.observability.RunMetrics;
import finance.project.api.repositories.RunErrorRepository;
import finance.project.api.repositories.RunExecutionRepository;
import finance.project.api.repositories.RunRequestRepository;
import finance.project.api.runs.RunDispatcher;
import finance.project.api.utils.HashingUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunRequestService {
    private static final Logger log = LoggerFactory.getLogger(RunRequestService.class);
    private static final DateTimeFormatter REQUEST_ID_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
            .withZone(ZoneOffset.UTC);

    private final RunRequestRepository runRequestRepository;
    private final RunExecutionRepository runExecutionRepository;
    private final RunErrorRepository runErrorRepository;
    private final PythonSpecService pythonSpecService;
    private final RunDispatcher runDispatcher;
    private final RunMetrics runMetrics;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RunRequestService(RunRequestRepository runRequestRepository,
                             RunExecutionRepository runExecutionRepository,
                             RunErrorRepository runErrorRepository,
                             PythonSpecService pythonSpecService,
                             RunDispatcher runDispatcher,
                             RunMetrics runMetrics,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this.runRequestRepository = runRequestRepository;
        this.runExecutionRepository = runExecutionRepository;
        this.runErrorRepository = runErrorRepository;
        this.pythonSpecService = pythonSpecService;
        this.runDispatcher = runDispatcher;
        this.runMetrics = runMetrics;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public RunSubmitResponse submit(RunRequestInput input) {
        long startNs = System.nanoTime();
        String requestId = normalizeRequestId(input.requestId());
        if (requestId != null) {
            Optional<RunRequestEntity> existing = runRequestRepository.findByRequestId(requestId);
            if (existing.isPresent()) {
                RunRequestEntity found = existing.get();
                MDC.put("requestId", found.getRequestId());
                MDC.put("specType", found.getSpecType());
                MDC.put("status", found.getStatus().name());
                MDC.put("latency_ms", String.valueOf((System.nanoTime() - startNs) / 1_000_000));
                log.info("run_reused");
                return new RunSubmitResponse(found.getRequestId(), found.getStatus());
            }
        } else {
            requestId = generateRequestId();
        }

        PythonSpec spec = pythonSpecService.buildSpec(input);

        String payloadJson = writeJson(input);
        String specJson = writeJson(spec);

        RunRequestEntity runRequest = RunRequestEntity.builder()
                .id(null)
                .requestId(requestId)
                .specType(input.specType())
                .catalogVersion(input.catalogVersion())
                .payloadIn(payloadJson)
                .payloadHash(HashingUtils.sha256Hex(payloadJson))
                .specGenerated(specJson)
                .specHash(HashingUtils.sha256Hex(specJson))
                .status(RunLifecycleStatus.PENDING)
                .build();

        runRequestRepository.save(runRequest);
        runMetrics.incrementRunsCreated(input.specType());
        RunExecutionEntity execution = runExecutionRepository.save(RunExecutionEntity.builder()
                .id(null)
                .requestId(requestId)
                .dispatchAttempts(0)
                .status(RunLifecycleStatus.PENDING)
                .build());

        MDC.put("requestId", requestId);
        MDC.put("specType", input.specType());
        MDC.put("status", runRequest.getStatus().name());
        MDC.put("latency_ms", String.valueOf((System.nanoTime() - startNs) / 1_000_000));
        log.info("run_created");

        try {
            runDispatcher.enqueue(requestId);
        } catch (Exception ex) {
            runRequest.setStatus(RunLifecycleStatus.DISPATCH_FAILED);
            runRequestRepository.save(runRequest);
            execution.setStatus(RunLifecycleStatus.DISPATCH_FAILED);
            execution.setLastDispatchError(ex.getMessage());
            runExecutionRepository.save(execution);
            runErrorRepository.save(RunErrorEntity.builder()
                    .requestId(requestId)
                    .source(RunErrorEntity.Source.DISPATCH)
                    .code("ENQUEUE_FAILED")
                    .message(ex.getMessage())
                    .build());
            runMetrics.incrementDispatchFailed();
            runMetrics.incrementRunsFailed("dispatch");
            MDC.put("status", runRequest.getStatus().name());
            log.warn("dispatch_enqueue_failed");
        }

        return new RunSubmitResponse(runRequest.getRequestId(), runRequest.getStatus());
    }

    private String generateRequestId() {
        Instant now = clock.instant();
        return "run_" + REQUEST_ID_FMT.format(now) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String normalizeRequestId(String requestId) {
        if (requestId == null) {
            return null;
        }
        String trimmed = requestId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize JSON", e);
        }
    }

}
