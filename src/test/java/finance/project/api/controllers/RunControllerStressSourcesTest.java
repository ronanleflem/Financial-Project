package finance.project.api.controllers;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.model.run.StressSourceRunItem;
import finance.project.api.model.run.StressSourceRunsResponse;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.CanonicalRunAuditService;
import finance.project.api.services.PythonCanonicalRunService;
import finance.project.api.services.PythonSpecService;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.services.StressSourceRunService;
import finance.project.api.validation.RunRequestValidator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = RunController.class, properties = "run.engine.mode=PYTHON_CANONICAL")
@Import({RunRequestValidator.class, RunValidationErrorHandler.class})
class RunControllerStressSourcesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PythonSpecService pythonSpecService;

    @MockBean
    private PythonCanonicalRunService pythonCanonicalRunService;

    @MockBean
    private CanonicalRunAuditService canonicalRunAuditService;

    @MockBean
    private RunRequestService runRequestService;

    @MockBean
    private RunStatusService runStatusService;

    @MockBean
    private RunResultService runResultService;

    @MockBean
    private StressSourceRunService stressSourceRunService;

    @MockBean
    private RunMetrics runMetrics;

    @Test
    void returnsEligibleStressSourcesWithCursor() throws Exception {
        StressSourceRunsResponse response = new StressSourceRunsResponse(
                List.of(new StressSourceRunItem(
                        "run_1",
                        "backtest",
                        "SUCCEEDED",
                        Instant.parse("2026-02-20T10:00:00Z"),
                        Instant.parse("2026-02-20T11:00:00Z"),
                        "SPY",
                        "1d",
                        "equity",
                        "USD",
                        42
                )),
                "cursor_abc"
        );
        org.mockito.Mockito.when(stressSourceRunService.listEligibleSources(20, "cursor_prev", "backtest"))
                .thenReturn(response);

        mockMvc.perform(get("/api/runs/stress/sources")
                        .param("limit", "20")
                        .param("cursor", "cursor_prev")
                        .param("strategyType", "backtest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].run_id", is("run_1")))
                .andExpect(jsonPath("$.items[0].spec_type", is("backtest")))
                .andExpect(jsonPath("$.items[0].status", is("SUCCEEDED")))
                .andExpect(jsonPath("$.items[0].symbol", is("SPY")))
                .andExpect(jsonPath("$.items[0].timeframe", is("1d")))
                .andExpect(jsonPath("$.items[0].asset_class", is("equity")))
                .andExpect(jsonPath("$.items[0].currency", is("USD")))
                .andExpect(jsonPath("$.items[0].trades_count_estimate", is(42)))
                .andExpect(jsonPath("$.next_cursor", is("cursor_abc")));
    }

    @Test
    void rejectsUnsupportedStrategyType() throws Exception {
        mockMvc.perform(get("/api/runs/stress/sources")
                        .param("strategyType", "seasonality"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.errors[0].field", is("strategyType")));

        org.mockito.Mockito.verifyNoInteractions(stressSourceRunService);
    }
}
