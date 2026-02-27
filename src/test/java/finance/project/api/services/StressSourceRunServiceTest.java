package finance.project.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.quant.ApiJobEntity;
import finance.project.api.model.run.StressSourceRunsResponse;
import finance.project.api.repositories.ApiJobRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class StressSourceRunServiceTest {

    @Test
    void keepsOnlyEligibleTypesAndParsesMetadata() {
        ApiJobRepository repository = org.mockito.Mockito.mock(ApiJobRepository.class);
        StressSourceRunService service = new StressSourceRunService(repository, new ObjectMapper());

        ApiJobEntity ignored = ApiJobEntity.builder()
                .jobId("run_2")
                .jobType("canonical_run")
                .status("SUCCEEDED")
                .payloadJson("{\"spec_type\":\"market_stats\",\"data\":{\"symbol\":\"QQQ\"}}")
                .createdAt(Instant.parse("2026-02-20T09:00:00Z"))
                .finishedAt(Instant.parse("2026-02-20T09:10:00Z"))
                .build();
        ApiJobEntity eligible = ApiJobEntity.builder()
                .jobId("run_1")
                .jobType("canonical_run")
                .status("SUCCEEDED")
                .payloadJson("{\"spec_type\":\"backtest\",\"data\":{\"symbol\":\"SPY\",\"timeframe\":\"1d\",\"asset_class\":\"equity\",\"currency\":\"USD\"}}")
                .resultJson("{\"summary\":{\"trades_count\":184}}")
                .createdAt(Instant.parse("2026-02-20T10:00:00Z"))
                .finishedAt(Instant.parse("2026-02-20T10:10:00Z"))
                .build();

        when(repository.findByJobTypeInAndStatusOrderByFinishedAtDescJobIdDesc(anyCollection(), eq("SUCCEEDED"), any()))
                .thenReturn(List.of(ignored, eligible));

        StressSourceRunsResponse response = service.listEligibleSources(1, null, null);

        assertEquals(1, response.items().size());
        assertEquals("run_1", response.items().get(0).runId());
        assertEquals("backtest", response.items().get(0).specType());
        assertEquals("SPY", response.items().get(0).symbol());
        assertEquals("1d", response.items().get(0).timeframe());
        assertEquals("equity", response.items().get(0).assetClass());
        assertEquals("USD", response.items().get(0).currency());
        assertEquals(184, response.items().get(0).tradesCountEstimate());
        assertEquals("run_1", StressSourceRunService.CursorToken.decode(response.nextCursor()).jobId());
    }

    @Test
    void appliesStrategyTypeFilter() {
        ApiJobRepository repository = org.mockito.Mockito.mock(ApiJobRepository.class);
        StressSourceRunService service = new StressSourceRunService(repository, new ObjectMapper());

        ApiJobEntity dca = ApiJobEntity.builder()
                .jobId("run_dca")
                .jobType("canonical_run")
                .status("SUCCEEDED")
                .payloadJson("{\"spec_type\":\"dca\"}")
                .finishedAt(Instant.parse("2026-02-21T10:00:00Z"))
                .build();
        ApiJobEntity backtest = ApiJobEntity.builder()
                .jobId("run_bt")
                .jobType("canonical_run")
                .status("SUCCEEDED")
                .payloadJson("{\"spec_type\":\"backtest\"}")
                .finishedAt(Instant.parse("2026-02-21T09:00:00Z"))
                .build();

        when(repository.findByJobTypeInAndStatusOrderByFinishedAtDescJobIdDesc(anyCollection(), eq("SUCCEEDED"), any()))
                .thenReturn(List.of(dca, backtest));

        StressSourceRunsResponse response = service.listEligibleSources(20, null, "dca");

        assertEquals(1, response.items().size());
        assertEquals("run_dca", response.items().get(0).runId());
        assertNull(response.nextCursor());
    }

    @Test
    void usesCursorQueryWhenCursorHasFinishedAt() {
        ApiJobRepository repository = org.mockito.Mockito.mock(ApiJobRepository.class);
        StressSourceRunService service = new StressSourceRunService(repository, new ObjectMapper());

        String cursor = StressSourceRunService.CursorToken
                .from(Instant.parse("2026-02-22T10:00:00Z"), "run_100")
                .encode();
        when(repository.findAfterCursorWithNonNullFinishedAt(anyCollection(), eq("SUCCEEDED"), any(), eq("run_100"), any()))
                .thenReturn(List.of());

        StressSourceRunsResponse response = service.listEligibleSources(20, cursor, null);

        assertEquals(0, response.items().size());
        assertNull(response.nextCursor());
        verify(repository).findAfterCursorWithNonNullFinishedAt(anyCollection(), eq("SUCCEEDED"), any(), eq("run_100"), any());
        verify(repository, never()).findByJobTypeInAndStatusOrderByFinishedAtDescJobIdDesc(anyCollection(), eq("SUCCEEDED"), any());
    }
}
