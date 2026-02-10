package finance.project.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.run.RunExecutionEntity;
import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.entities.run.RunRequestEntity;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.RunSubmitResponse;
import finance.project.api.model.run.BacktestDataBlock;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.RunType;
import finance.project.api.observability.RunMetrics;
import finance.project.api.repositories.RunErrorRepository;
import finance.project.api.repositories.RunExecutionRepository;
import finance.project.api.repositories.RunRequestRepository;
import finance.project.api.runs.RunDispatcher;
import finance.project.api.utils.HashingUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(RunRequestServiceTest.Config.class)
class RunRequestServiceTest {

    @TestConfiguration
    static class Config {
        @Bean
        RunRequestService runRequestService(RunRequestRepository repository,
                                            RunExecutionRepository runExecutionRepository,
                                            RunErrorRepository runErrorRepository,
                                            PythonSpecService pythonSpecService,
                                            RunDispatcher runDispatcher,
                                            RunMetrics runMetrics,
                                            ObjectMapper objectMapper,
                                            Clock clock) {
            return new RunRequestService(repository, runExecutionRepository, runErrorRepository, pythonSpecService, runDispatcher, runMetrics, objectMapper, clock);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-02-09T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private RunRequestService runRequestService;

    @Autowired
    private RunRequestRepository runRequestRepository;

    @Autowired
    private RunExecutionRepository runExecutionRepository;

    @MockBean
    private PythonSpecService pythonSpecService;

    @MockBean
    private RunDispatcher runDispatcher;

    @MockBean
    private RunMetrics runMetrics;

    @Test
    void persistsPendingRunAndReturnsRequestId() {
        org.mockito.Mockito.when(pythonSpecService.buildSpec(org.mockito.Mockito.any()))
                .thenReturn(new PythonSpec("backtest", Map.of("data", Map.of())));

        RunRequestInput input = new RunRequestInput(
                "backtest",
                "2026-02-02",
                null,
                RunType.BACKTEST,
                new BacktestDataBlock("SPY", "1d", "2022-01-01", "2023-01-01", "Breakout"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        RunSubmitResponse response = runRequestService.submit(input);

        assertNotNull(response.requestId());
        assertEquals(RunLifecycleStatus.PENDING, response.status());

        Optional<RunRequestEntity> stored = runRequestRepository.findByRequestId(response.requestId());
        assertTrue(stored.isPresent());
        assertEquals("backtest", stored.get().getSpecType());
        assertEquals("2026-02-02", stored.get().getCatalogVersion());
        assertEquals(RunLifecycleStatus.PENDING, stored.get().getStatus());
        assertNotNull(stored.get().getPayloadIn());
        assertNotNull(stored.get().getSpecGenerated());
        assertEquals(HashingUtils.sha256Hex(stored.get().getPayloadIn()), stored.get().getPayloadHash());
        assertEquals(HashingUtils.sha256Hex(stored.get().getSpecGenerated()), stored.get().getSpecHash());

        Optional<RunExecutionEntity> exec = runExecutionRepository.findByRequestId(response.requestId());
        assertTrue(exec.isPresent());
        assertEquals(RunLifecycleStatus.PENDING, exec.get().getStatus());
    }

    @Test
    void marksDispatchFailedWhenEnqueueThrows() {
        org.mockito.Mockito.when(pythonSpecService.buildSpec(org.mockito.Mockito.any()))
                .thenReturn(new PythonSpec("backtest", Map.of("data", Map.of())));
        org.mockito.Mockito.doThrow(new RuntimeException("python down"))
                .when(runDispatcher).enqueue(org.mockito.Mockito.anyString());

        RunRequestInput input = new RunRequestInput(
                "backtest",
                "2026-02-02",
                null,
                RunType.BACKTEST,
                new BacktestDataBlock("SPY", "1d", "2022-01-01", "2023-01-01", "Breakout"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        RunSubmitResponse response = runRequestService.submit(input);

        assertEquals(RunLifecycleStatus.DISPATCH_FAILED, response.status());
        RunRequestEntity stored = runRequestRepository.findByRequestId(response.requestId()).orElseThrow();
        assertEquals(RunLifecycleStatus.DISPATCH_FAILED, stored.getStatus());
        RunExecutionEntity exec = runExecutionRepository.findByRequestId(response.requestId()).orElseThrow();
        assertEquals(RunLifecycleStatus.DISPATCH_FAILED, exec.getStatus());
        assertEquals("python down", exec.getLastDispatchError());
    }

    @Test
    void isIdempotentWhenRequestIdProvided() {
        org.mockito.Mockito.when(pythonSpecService.buildSpec(org.mockito.Mockito.any()))
                .thenReturn(new PythonSpec("backtest", Map.of("data", Map.of())));

        RunRequestInput input = new RunRequestInput(
                "backtest",
                "2026-02-02",
                "req-1",
                RunType.BACKTEST,
                new BacktestDataBlock("SPY", "1d", "2022-01-01", "2023-01-01", "Breakout"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        RunSubmitResponse first = runRequestService.submit(input);
        RunSubmitResponse second = runRequestService.submit(input);

        assertEquals("req-1", first.requestId());
        assertEquals(first, second);
        org.mockito.Mockito.verify(runDispatcher, org.mockito.Mockito.times(1)).enqueue(org.mockito.Mockito.anyString());
    }
}
