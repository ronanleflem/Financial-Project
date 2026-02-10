package finance.project.api.services;

import finance.project.api.entities.run.RunExecutionEntity;
import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.entities.run.RunRequestEntity;
import finance.project.api.repositories.RunExecutionRepository;
import finance.project.api.repositories.RunRequestRepository;
import finance.project.api.runs.PythonDispatchException;
import finance.project.api.runs.PythonJobStatus;
import finance.project.api.runs.PythonJobStatusClient;
import finance.project.api.utils.HashingUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Import(RunStatusServiceTest.Config.class)
class RunStatusServiceTest {

    @TestConfiguration
    static class Config {
        @Bean
        InMemoryPythonJobStatusClient pythonJobStatusClient() {
            return new InMemoryPythonJobStatusClient();
        }

        @Bean
        RunStatusService runStatusService(RunRequestRepository runRequestRepository,
                                          RunExecutionRepository runExecutionRepository,
                                          PythonJobStatusClient pythonJobStatusClient) {
            return new RunStatusService(runRequestRepository, runExecutionRepository, pythonJobStatusClient);
        }
    }

    static final class InMemoryPythonJobStatusClient implements PythonJobStatusClient {
        private final Map<String, PythonJobStatus> statuses = new ConcurrentHashMap<>();
        private volatile RuntimeException exception;

        void setStatus(String jobId, PythonJobStatus status) {
            statuses.put(jobId, status);
        }

        void setException(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public PythonJobStatus getStatus(String jobId) {
            if (exception != null) {
                throw exception;
            }
            return statuses.getOrDefault(jobId, PythonJobStatus.UNKNOWN);
        }
    }

    @Autowired
    private RunRequestRepository runRequestRepository;

    @Autowired
    private RunExecutionRepository runExecutionRepository;

    @Autowired
    private RunStatusService runStatusService;

    @Autowired
    private InMemoryPythonJobStatusClient pythonJobStatusClient;

    @Test
    void pendingWithoutJobIdReturnsPending() {
        persistRequest("run-1", RunLifecycleStatus.PENDING);

        var response = runStatusService.getStatus("run-1");
        assertEquals("run-1", response.requestId());
        assertEquals(finance.project.api.model.RunStatusResponse.Status.PENDING, response.status());
        assertNotNull(response.updatedAt());
    }

    @Test
    void dispatchFailedNormalizesToFailed() {
        persistRequest("run-2", RunLifecycleStatus.DISPATCH_FAILED);
        runExecutionRepository.saveAndFlush(RunExecutionEntity.builder()
                .requestId("run-2")
                .dispatchAttempts(1)
                .status(RunLifecycleStatus.DISPATCH_FAILED)
                .lastDispatchError("HTTP 500 from python")
                .build());

        var response = runStatusService.getStatus("run-2");
        assertEquals(finance.project.api.model.RunStatusResponse.Status.FAILED, response.status());
        assertEquals("HTTP 500 from python", response.message());
    }

    @Test
    void runningWithPythonQueuedNormalizesToRunning() {
        pythonJobStatusClient.setStatus("py-1", PythonJobStatus.QUEUED);
        persistRequest("run-3", RunLifecycleStatus.RUNNING);
        runExecutionRepository.saveAndFlush(RunExecutionEntity.builder()
                .requestId("run-3")
                .pythonJobId("py-1")
                .dispatchAttempts(1)
                .status(RunLifecycleStatus.RUNNING)
                .build());

        var response = runStatusService.getStatus("run-3");
        assertEquals(finance.project.api.model.RunStatusResponse.Status.RUNNING, response.status());
    }

    @Test
    void runningWithPythonDoneNormalizesToCompleted() {
        pythonJobStatusClient.setStatus("py-2", PythonJobStatus.DONE);
        persistRequest("run-4", RunLifecycleStatus.RUNNING);
        runExecutionRepository.saveAndFlush(RunExecutionEntity.builder()
                .requestId("run-4")
                .pythonJobId("py-2")
                .dispatchAttempts(1)
                .status(RunLifecycleStatus.RUNNING)
                .build());

        var response = runStatusService.getStatus("run-4");
        assertEquals(finance.project.api.model.RunStatusResponse.Status.COMPLETED, response.status());
    }

    @Test
    void runningWithPythonFailedNormalizesToFailed() {
        pythonJobStatusClient.setStatus("py-3", PythonJobStatus.FAILED);
        persistRequest("run-5", RunLifecycleStatus.RUNNING);
        runExecutionRepository.saveAndFlush(RunExecutionEntity.builder()
                .requestId("run-5")
                .pythonJobId("py-3")
                .dispatchAttempts(1)
                .status(RunLifecycleStatus.RUNNING)
                .build());

        var response = runStatusService.getStatus("run-5");
        assertEquals(finance.project.api.model.RunStatusResponse.Status.FAILED, response.status());
    }

    @Test
    void pythonUnavailableFallsBackToJavaStatusAndAddsMessage() {
        pythonJobStatusClient.setException(new PythonDispatchException("timeout", null, true));
        persistRequest("run-6", RunLifecycleStatus.RUNNING);
        runExecutionRepository.saveAndFlush(RunExecutionEntity.builder()
                .requestId("run-6")
                .pythonJobId("py-4")
                .dispatchAttempts(1)
                .status(RunLifecycleStatus.RUNNING)
                .build());

        var response = runStatusService.getStatus("run-6");
        assertEquals(finance.project.api.model.RunStatusResponse.Status.RUNNING, response.status());
        assertEquals("Python status unavailable", response.message());
    }

    private void persistRequest(String requestId, RunLifecycleStatus status) {
        String payload = "{}";
        String spec = "{\"spec_type\":\"backtest\"}";
        runRequestRepository.saveAndFlush(RunRequestEntity.builder()
                .requestId(requestId)
                .specType("backtest")
                .catalogVersion("2026-02-02")
                .payloadIn(payload)
                .payloadHash(HashingUtils.sha256Hex(payload))
                .specGenerated(spec)
                .specHash(HashingUtils.sha256Hex(spec))
                .status(status)
                .build());
    }
}

