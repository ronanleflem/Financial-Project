package finance.project.api.runs;

import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.entities.run.RunRequestEntity;
import finance.project.api.observability.RunMetrics;
import finance.project.api.repositories.RunErrorRepository;
import finance.project.api.repositories.RunExecutionRepository;
import finance.project.api.repositories.RunRequestRepository;
import finance.project.api.utils.HashingUtils;
import java.time.Clock;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Import(DefaultRunDispatcherTest.Config.class)
class DefaultRunDispatcherTest {

    @TestConfiguration
    static class Config {
        @Bean(name = "runDispatchExecutor")
        Executor directExecutor() {
            return Runnable::run;
        }

        @Bean
        DispatchBackoff noBackoff() {
            return attempt -> {
            };
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        PythonAsyncClient pythonAsyncClient() {
            return new PythonAsyncClient() {
                private int calls = 0;

                @Override
                public String submitAsync(String requestId, String specJson) {
                    calls++;
                    if (calls < 3) {
                        throw new PythonDispatchException("HTTP 500 from python", 500, true);
                    }
                    return "py_123";
                }
            };
        }

        @Bean
        DefaultRunDispatcher runDispatcher(Executor runDispatchExecutor,
                                           RunRequestRepository runRequestRepository,
                                           RunExecutionRepository runExecutionRepository,
                                           RunErrorRepository runErrorRepository,
                                           PythonAsyncClient pythonAsyncClient,
                                           DispatchBackoff dispatchBackoff,
                                           Clock clock,
                                           RunMetrics runMetrics) {
            return new DefaultRunDispatcher(
                    runDispatchExecutor,
                    runRequestRepository,
                    runExecutionRepository,
                    runErrorRepository,
                    pythonAsyncClient,
                    dispatchBackoff,
                    clock,
                    runMetrics
            );
        }
    }

    @Autowired
    private DefaultRunDispatcher runDispatcher;

    @Autowired
    private RunRequestRepository runRequestRepository;

    @Autowired
    private RunExecutionRepository runExecutionRepository;

    @Autowired
    private RunErrorRepository runErrorRepository;

    @MockBean
    private RunMetrics runMetrics;

    @Test
    void retriesAndMarksRunningOnSuccess() {
        String spec = "{\"spec_type\":\"backtest\"}";
        String payload = "{}";
        RunRequestEntity req = RunRequestEntity.builder()
                .requestId("run-1")
                .specType("backtest")
                .catalogVersion("2026-02-02")
                .payloadIn(payload)
                .payloadHash(HashingUtils.sha256Hex(payload))
                .specGenerated(spec)
                .specHash(HashingUtils.sha256Hex(spec))
                .status(RunLifecycleStatus.PENDING)
                .build();
        runRequestRepository.save(req);

        runDispatcher.enqueue("run-1");

        RunRequestEntity stored = runRequestRepository.findByRequestId("run-1").orElseThrow();
        assertEquals(RunLifecycleStatus.RUNNING, stored.getStatus());

        var exec = runExecutionRepository.findByRequestId("run-1").orElseThrow();
        assertEquals(RunLifecycleStatus.RUNNING, exec.getStatus());
        assertEquals(3, exec.getDispatchAttempts());
        assertEquals("py_123", exec.getPythonJobId());
    }

    @Test
    void doesNotRetryOnHttp400() {
        PythonAsyncClient client400 = (requestId, specJson) -> {
            throw new PythonDispatchException("HTTP 400 from python", 400, false);
        };

        DefaultRunDispatcher dispatcher = new DefaultRunDispatcher(
                Runnable::run,
                runRequestRepository,
                runExecutionRepository,
                runErrorRepository,
                client400,
                attempt -> {
                },
                Clock.systemUTC(),
                runMetrics
        );

        String spec = "{\"spec_type\":\"backtest\"}";
        String payload = "{}";
        RunRequestEntity req = RunRequestEntity.builder()
                .requestId("run-2")
                .specType("backtest")
                .catalogVersion("2026-02-02")
                .payloadIn(payload)
                .payloadHash(HashingUtils.sha256Hex(payload))
                .specGenerated(spec)
                .specHash(HashingUtils.sha256Hex(spec))
                .status(RunLifecycleStatus.PENDING)
                .build();
        runRequestRepository.save(req);

        dispatcher.enqueue("run-2");

        RunRequestEntity stored = runRequestRepository.findByRequestId("run-2").orElseThrow();
        assertEquals(RunLifecycleStatus.DISPATCH_FAILED, stored.getStatus());

        var exec = runExecutionRepository.findByRequestId("run-2").orElseThrow();
        assertEquals(RunLifecycleStatus.DISPATCH_FAILED, exec.getStatus());
        assertEquals(1, exec.getDispatchAttempts());
        assertNotNull(exec.getLastDispatchError());

        assertEquals(1, runErrorRepository.findByRequestIdOrderByCreatedAtAsc("run-2").size());
    }
}
