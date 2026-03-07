package finance.project.api.services.marketanalysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.quant.ApiJobEntity;
import finance.project.api.entities.quant.MarketStatsEntity;
import finance.project.api.model.marketanalysis.MarketAnalysisRunDetailResponse;
import finance.project.api.model.marketanalysis.MarketAnalysisRunResultResponse;
import finance.project.api.repositories.ApiJobRepository;
import finance.project.api.repositories.MarketStatsRepository;
import finance.project.api.repositories.SeasonalityProfileRepository;
import finance.project.api.repositories.SeasonalityRunRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketAnalysisServiceTest {

    @Mock
    private ApiJobRepository apiJobRepository;

    @Mock
    private MarketStatsRepository marketStatsRepository;

    @Mock
    private SeasonalityRunRepository seasonalityRunRepository;

    @Mock
    private SeasonalityProfileRepository seasonalityProfileRepository;

    private MarketAnalysisService marketAnalysisService;

    @BeforeEach
    void setUp() {
        marketAnalysisService = new MarketAnalysisService(
                apiJobRepository,
                marketStatsRepository,
                seasonalityRunRepository,
                seasonalityProfileRepository,
                new ObjectMapper()
        );
    }

    @Test
    void getRunResultTreatsSucceededAsTerminalWhenUsingResultJson() {
        ApiJobEntity job = job("run-succeeded", "SUCCEEDED", """
                {"request":{"spec_type":"seasonality","spec_id":"spec-1","dataset_id":"dataset-1","data":{"window":"90d"}}}
                """, """
                {"spec_id":"spec-1","dataset_id":"dataset-1","score":1}
                """);
        when(apiJobRepository.findByJobId("run-succeeded")).thenReturn(Optional.of(job));
        when(seasonalityRunRepository.findByRunId("run-succeeded")).thenReturn(Optional.empty());
        when(seasonalityProfileRepository.findBySpecIdAndDatasetIdOrderByCreatedAtAsc("spec-1", "dataset-1"))
                .thenReturn(List.of());

        MarketAnalysisRunResultResponse response = marketAnalysisService.getRunResult("run-succeeded");

        assertEquals("result_json", response.source());
        assertEquals("succeeded", response.meta().status());
        assertNotNull(response.data().rawResultJson());
        assertEquals("spec-1", response.meta().specId());
    }

    @Test
    void getRunResultUsesPersistedTablesForSucceededRunWhenRowsExist() {
        ApiJobEntity job = job("run-persisted", "succeeded", """
                {"request":{"spec_type":"market_stats","spec_id":"spec-2","dataset_id":"dataset-2"}}
                """, null);
        when(apiJobRepository.findByJobId("run-persisted")).thenReturn(Optional.of(job));
        when(marketStatsRepository.findBySpecIdAndDatasetIdOrderByCreatedAtAsc("spec-2", "dataset-2"))
                .thenReturn(List.of(MarketStatsEntity.builder()
                        .symbol("BTCUSD")
                        .timeframe("1d")
                        .event("breakout")
                        .target("up")
                        .split("train")
                        .n(10)
                        .successes(6)
                        .pHat(0.6)
                        .pMean(0.58)
                        .pMap(0.57)
                        .hdiLow(0.45)
                        .hdiHigh(0.68)
                        .lift(1.2)
                        .liftFreq(1.15)
                        .liftBayes(1.11)
                        .pValue(0.03)
                        .qValue(0.04)
                        .significant(true)
                        .insufficient(false)
                        .start("2025-01-01")
                        .end("2025-03-01")
                        .specId("spec-2")
                        .datasetId("dataset-2")
                        .build()));

        MarketAnalysisRunResultResponse response = marketAnalysisService.getRunResult("run-persisted");

        assertEquals("persisted_tables", response.source());
        assertEquals(null, response.meta().window());
        assertEquals("2025-01-01", response.meta().start());
        assertEquals("2025-03-01", response.meta().end());
        assertEquals("BTCUSD", response.meta().symbol());
        assertEquals("1d", response.meta().timeframe());
        assertEquals("breakout", response.meta().event());
        assertEquals("up", response.meta().target());
        assertEquals(1, response.meta().rowCount());
        assertEquals(1, response.data().marketStatsRows().size());
        assertEquals(0.58, response.data().marketStatsRows().getFirst().pMean());
        assertEquals(1.11, response.data().marketStatsRows().getFirst().liftBayes());
        assertEquals(true, response.data().marketStatsRows().getFirst().significant());
    }

    @Test
    void getRunResultKeepsLegacyRowsReadableWhenEnrichedColumnsAreNull() {
        ApiJobEntity job = job("run-legacy", "done", """
                {"request":{"spec_type":"market_stats","spec_id":"spec-legacy","dataset_id":"dataset-legacy"}}
                """, null);
        when(apiJobRepository.findByJobId("run-legacy")).thenReturn(Optional.of(job));
        when(marketStatsRepository.findBySpecIdAndDatasetIdOrderByCreatedAtAsc("spec-legacy", "dataset-legacy"))
                .thenReturn(List.of(MarketStatsEntity.builder()
                        .symbol("SPY")
                        .timeframe("1d")
                        .event("gap_up")
                        .conditionName("session")
                        .conditionValue("RTH")
                        .target("continuation")
                        .split("test")
                        .n(8)
                        .successes(5)
                        .pHat(0.625)
                        .ciLow(0.40)
                        .ciHigh(0.81)
                        .lift(1.05)
                        .start("2024-01-01")
                        .end("2024-06-01")
                        .specId("spec-legacy")
                        .datasetId("dataset-legacy")
                        .build()));

        MarketAnalysisRunResultResponse response = marketAnalysisService.getRunResult("run-legacy");

        assertEquals("persisted_tables", response.source());
        assertEquals("SPY", response.meta().symbol());
        assertEquals("1d", response.meta().timeframe());
        assertEquals("gap_up", response.meta().event());
        assertEquals("session", response.meta().condition());
        assertEquals("continuation", response.meta().target());
        assertEquals(1, response.meta().rowCount());
        assertEquals(1, response.data().marketStatsRows().size());
        assertNull(response.data().marketStatsRows().getFirst().pMean());
        assertNull(response.data().marketStatsRows().getFirst().liftBayes());
        assertNull(response.data().marketStatsRows().getFirst().significant());
        assertNull(response.data().marketStatsRows().getFirst().insufficient());
    }

    @Test
    void getRunResultBuildsUsefulMarketStatsMetaFromResultJsonFallback() {
        ApiJobEntity job = job("run-result-json", "DONE", """
                {"request":{"spec_type":"market_stats","spec_id":"spec-json","dataset_id":"dataset-json","data":{"symbols":["QQQ"],"timeframe":"4h","stats_pack":"Liquidity"},"stats":{"event":{"id":"gap_up"},"condition":{"id":"session"},"target":{"id":"retracement_probability"}}}}
                """, """
                {"meta":{"row_count":2},"market_stats_rows":[{"start":"2024-01-01","end":"2024-01-31"},{"start":"2024-02-01","end":"2024-03-31"}]}
                """);
        when(apiJobRepository.findByJobId("run-result-json")).thenReturn(Optional.of(job));
        when(marketStatsRepository.findBySpecIdAndDatasetIdOrderByCreatedAtAsc("spec-json", "dataset-json"))
                .thenReturn(List.of());

        MarketAnalysisRunResultResponse response = marketAnalysisService.getRunResult("run-result-json");

        assertEquals("result_json", response.source());
        assertNull(response.meta().window());
        assertEquals("QQQ", response.meta().symbol());
        assertEquals("4h", response.meta().timeframe());
        assertEquals("Liquidity", response.meta().statsPack());
        assertEquals("gap_up", response.meta().event());
        assertEquals("session", response.meta().condition());
        assertEquals("retracement_probability", response.meta().target());
        assertEquals("2024-01-01", response.meta().start());
        assertEquals("2024-03-31", response.meta().end());
        assertEquals(2, response.meta().rowCount());
    }

    @Test
    void getRunResultReturnsConflictForNonTerminalRunWithoutResult() {
        ApiJobEntity job = job("run-running", "RUNNING", """
                {"request":{"spec_type":"market_stats","spec_id":"spec-3","dataset_id":"dataset-3"}}
                """, null);
        when(apiJobRepository.findByJobId("run-running")).thenReturn(Optional.of(job));

        assertThrows(
                MarketAnalysisResultNotReadyException.class,
                () -> marketAnalysisService.getRunResult("run-running")
        );
    }

    @Test
    void malformedResultJsonDoesNotReportAvailabilityInRunDetail() {
        ApiJobEntity job = job("run-malformed", "DONE", """
                {"request":{"spec_type":"seasonality","spec_id":"spec-4","dataset_id":"dataset-4"}}
                """, "{not-json");
        when(apiJobRepository.findByJobId("run-malformed")).thenReturn(Optional.of(job));
        when(seasonalityRunRepository.findByRunId("run-malformed")).thenReturn(Optional.empty());
        when(seasonalityProfileRepository.findBySpecIdAndDatasetIdOrderByCreatedAtAsc("spec-4", "dataset-4"))
                .thenReturn(List.of());

        MarketAnalysisRunDetailResponse detail = marketAnalysisService.getRunDetail("run-malformed");
        MarketAnalysisRunResultResponse result = marketAnalysisService.getRunResult("run-malformed");

        assertFalse(detail.resultJsonAvailable());
        assertEquals("result_json", result.source());
        assertTrue(result.data().marketStatsRows().isEmpty());
        assertTrue(result.data().seasonalityProfiles().isEmpty());
        assertNull(result.data().rawResultJson());
    }

    private ApiJobEntity job(String runId, String status, String payloadJson, String resultJson) {
        return ApiJobEntity.builder()
                .jobId(runId)
                .jobType("canonical_run")
                .status(status)
                .payloadJson(payloadJson)
                .resultJson(resultJson)
                .attempts(1)
                .cancelRequested(false)
                .build();
    }
}
