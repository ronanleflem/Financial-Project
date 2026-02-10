package finance.project.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import finance.project.api.entities.run.RunArtifactEntity;
import finance.project.api.entities.run.RunErrorEntity;
import finance.project.api.entities.run.RunExecutionEntity;
import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.entities.run.RunRequestEntity;
import finance.project.api.model.RunStatusResponse;
import finance.project.api.repositories.RunArtifactRepository;
import finance.project.api.repositories.RunErrorRepository;
import finance.project.api.repositories.RunExecutionRepository;
import finance.project.api.repositories.RunRequestRepository;
import finance.project.api.runs.PythonJobResultClient;
import finance.project.api.utils.HashingUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@Import(RunResultServiceTest.Config.class)
class RunResultServiceTest {

    @TestConfiguration
    static class Config {
        @Bean
        PythonJobResultClient pythonJobResultClient() {
            return jobId -> new ObjectMapper().createObjectNode();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        RunStatusService runStatusService(RunRequestRepository runRequestRepository,
                                          RunExecutionRepository runExecutionRepository) {
            return new RunStatusService(runRequestRepository, runExecutionRepository, jobId -> finance.project.api.runs.PythonJobStatus.UNKNOWN);
        }

        @Bean
        RunResultService runResultService(RunRequestRepository runRequestRepository,
                                          RunExecutionRepository runExecutionRepository,
                                          RunErrorRepository runErrorRepository,
                                          RunArtifactRepository runArtifactRepository,
                                          RunStatusService runStatusService,
                                          PythonJobResultClient pythonJobResultClient,
                                          ObjectMapper objectMapper) {
            return new RunResultService(
                    runRequestRepository,
                    runExecutionRepository,
                    runErrorRepository,
                    runArtifactRepository,
                    runStatusService,
                    pythonJobResultClient,
                    objectMapper
            );
        }
    }

    @Autowired
    private RunRequestRepository runRequestRepository;

    @Autowired
    private RunExecutionRepository runExecutionRepository;

    @Autowired
    private RunArtifactRepository runArtifactRepository;

    @Autowired
    private RunErrorRepository runErrorRepository;

    @Autowired
    private RunResultService runResultService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completedReturnsStoredResultArtifactsWarnings() throws Exception {
        persistRequest("run-1", RunLifecycleStatus.DONE);

        ObjectNode result = objectMapper.createObjectNode().put("a", 1);
        runExecutionRepository.saveAndFlush(RunExecutionEntity.builder()
                .requestId("run-1")
                .dispatchAttempts(1)
                .status(RunLifecycleStatus.DONE)
                .resultJson(objectMapper.writeValueAsString(result))
                .warningsJson("""
                        ["w1","w2"]
                        """)
                .build());

        runArtifactRepository.saveAndFlush(RunArtifactEntity.builder()
                .requestId("run-1")
                .type("parquet")
                .path("/tmp/a.parquet")
                .meta("{\"rows\":10}")
                .build());

        var response = runResultService.getResult("run-1");
        assertEquals("run-1", response.requestId());
        assertEquals(RunStatusResponse.Status.COMPLETED, response.status());
        assertEquals(1, response.result().get("a").asInt());
        assertEquals(1, response.artifacts().size());
        assertEquals("/tmp/a.parquet", response.artifacts().getFirst().path());
        assertEquals(List.of("w1", "w2"), response.warnings());
        assertNull(response.error());
    }

    @Test
    void failedReturnsPythonExecError() {
        persistRequest("run-2", RunLifecycleStatus.FAILED);

        runErrorRepository.saveAndFlush(RunErrorEntity.builder()
                .requestId("run-2")
                .source(RunErrorEntity.Source.PYTHON)
                .code("PYTHON_EXEC_ERROR")
                .message("boom")
                .build());

        var response = runResultService.getResult("run-2");
        assertEquals(RunStatusResponse.Status.FAILED, response.status());
        assertNotNull(response.error());
        assertEquals("PYTHON_EXEC_ERROR", response.error().code());
        assertEquals("boom", response.error().message());
    }

    @Test
    void runningReturnsNotAvailableMessage() {
        persistRequest("run-3", RunLifecycleStatus.RUNNING);

        var response = runResultService.getResult("run-3");
        assertEquals(RunStatusResponse.Status.RUNNING, response.status());
        assertEquals("Result not available yet", response.message());
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

