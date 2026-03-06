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
        assertEquals("2025-01-01", response.meta().start());
        assertEquals("2025-03-01", response.meta().end());
        assertEquals(1, response.data().marketStatsRows().size());
        assertEquals(0.58, response.data().marketStatsRows().getFirst().pMean());
        assertEquals(1.11, response.data().marketStatsRows().getFirst().liftBayes());
        assertEquals(true, response.data().marketStatsRows().getFirst().significant());
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
