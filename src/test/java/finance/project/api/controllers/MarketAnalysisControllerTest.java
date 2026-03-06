package finance.project.api.controllers;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finance.project.api.model.marketanalysis.MarketAnalysisResultData;
import finance.project.api.model.marketanalysis.MarketAnalysisMarketStatsRow;
import finance.project.api.model.marketanalysis.MarketAnalysisResultMeta;
import finance.project.api.model.marketanalysis.MarketAnalysisRunItem;
import finance.project.api.model.marketanalysis.MarketAnalysisRunListResponse;
import finance.project.api.model.marketanalysis.MarketAnalysisRunResultResponse;
import finance.project.api.services.marketanalysis.MarketAnalysisNotFoundException;
import finance.project.api.services.marketanalysis.MarketAnalysisResultNotReadyException;
import finance.project.api.services.marketanalysis.MarketAnalysisService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketAnalysisController.class)
class MarketAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketAnalysisService marketAnalysisService;

    @Test
    void returnsRunList() throws Exception {
        MarketAnalysisRunItem item = new MarketAnalysisRunItem(
                "run_1", "req_1", "market_stats", "done",
                Instant.parse("2026-03-05T10:00:00Z"),
                null, null, null, null,
                1, 3, false, true, "spec-1", "dataset-1"
        );
        Mockito.when(marketAnalysisService.listRuns(
                        Mockito.eq("market_stats"),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.eq(0),
                        Mockito.eq(20),
                        Mockito.eq("created_at,desc")))
                .thenReturn(new MarketAnalysisRunListResponse(List.of(item), 0, 20, 1, 1, "created_at,desc"));

        mockMvc.perform(get("/api/market-analysis/runs")
                        .param("spec_type", "market_stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].run_id", is("run_1")))
                .andExpect(jsonPath("$.items[0].spec_type", is("market_stats")))
                .andExpect(jsonPath("$.items[0].persistence_enabled", is(true)));
    }

    @Test
    void returnsResultFromResultJsonSource() throws Exception {
        MarketAnalysisRunResultResponse response = new MarketAnalysisRunResultResponse(
                "run_2",
                "seasonality",
                "result_json",
                new MarketAnalysisResultMeta("spec-2", "dataset-2", null, "90d", null, null, "done"),
                new MarketAnalysisResultData(List.of(), List.of(), null, null)
        );
        Mockito.when(marketAnalysisService.getRunResult("run_2")).thenReturn(response);

        mockMvc.perform(get("/api/market-analysis/runs/run_2/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source", is("result_json")))
                .andExpect(jsonPath("$.spec_type", is("seasonality")));
    }

    @Test
    void returnsResultFromPersistedTablesSource() throws Exception {
        MarketAnalysisRunResultResponse response = new MarketAnalysisRunResultResponse(
                "run_3",
                "market_stats",
                "persisted_tables",
                new MarketAnalysisResultMeta("spec-3", "dataset-3", null, null, "2025-01-01", "2025-03-01", "done"),
                new MarketAnalysisResultData(List.of(
                        new MarketAnalysisMarketStatsRow(
                                "BTCUSD",
                                "1d",
                                "breakout",
                                "session",
                                "RTH",
                                "up",
                                "train",
                                10,
                                6,
                                0.6,
                                0.45,
                                0.72,
                                1.2,
                                0.58,
                                0.57,
                                0.49,
                                0.66,
                                1.15,
                                1.11,
                                0.03,
                                0.04,
                                true,
                                false,
                                "2025-01-01",
                                "2025-03-01",
                                "spec-3",
                                "dataset-3",
                                Instant.parse("2026-03-05T10:00:00Z")
                        )
                ), List.of(), null, null)
        );
        Mockito.when(marketAnalysisService.getRunResult("run_3")).thenReturn(response);

        mockMvc.perform(get("/api/market-analysis/runs/run_3/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source", is("persisted_tables")))
                .andExpect(jsonPath("$.meta.spec_id", is("spec-3")))
                .andExpect(jsonPath("$.data.market_stats_rows[0].p_mean", is(0.58)))
                .andExpect(jsonPath("$.data.market_stats_rows[0].lift_bayes", is(1.11)))
                .andExpect(jsonPath("$.data.market_stats_rows[0].significant", is(true)))
                .andExpect(jsonPath("$.data.market_stats_rows[0].insufficient", is(false)));
    }

    @Test
    void returnsNotFoundWhenRunMissing() throws Exception {
        Mockito.when(marketAnalysisService.getRunResult("missing"))
                .thenThrow(new MarketAnalysisNotFoundException("run not found: missing"));

        mockMvc.perform(get("/api/market-analysis/runs/missing/result"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RUN_NOT_FOUND")));
    }

    @Test
    void returnsConflictWhenResultNotReady() throws Exception {
        Mockito.when(marketAnalysisService.getRunResult("run_4"))
                .thenThrow(new MarketAnalysisResultNotReadyException("result not ready for run: run_4"));

        mockMvc.perform(get("/api/market-analysis/runs/run_4/result"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESULT_NOT_READY")));
    }

    @Test
    void returns422OnInvalidSpecType() throws Exception {
        mockMvc.perform(get("/api/market-analysis/runs")
                        .param("spec_type", "invalid"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field", is("spec_type")))
                .andExpect(jsonPath("$.errors[0].code", is("INVALID_SPEC_TYPE")));
    }
}
