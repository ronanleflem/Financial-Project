package finance.project.api.controllers;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.model.PythonSpec;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.PythonSpecService;
import finance.project.api.services.PythonCanonicalRunService;
import finance.project.api.services.CanonicalRunAuditService;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.validation.RunRequestValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunController.class)
@Import({RunRequestValidator.class, RunValidationErrorHandler.class})
class RunControllerValidationTest {

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
    private RunMetrics runMetrics;

    @Test
    void validatesBeanConstraints() throws Exception {
        org.mockito.Mockito.when(pythonSpecService.buildSpec(org.mockito.Mockito.any()))
                .thenReturn(new PythonSpec("backtest", java.util.Map.of()));
        String json = """
                {
                  "specType": "backtest",
                  "catalogVersion": "2026-02-02",
                  "runType": "backtest",
                  "data": {
                    "symbol": "SPY",
                    "timeframe": "1d",
                    "startDate": "2022-01-01",
                    "endDate": "2023-01-01",
                    "strategyName": "Breakout"
                  },
                  "signal": {
                    "type": "ema_cross",
                    "fast": 0,
                    "slow": 2,
                    "requireCrossing": true
                  }
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("signal.params.fast")));
    }

    @Test
    void validatesCrossRules() throws Exception {
        org.mockito.Mockito.when(pythonSpecService.buildSpec(org.mockito.Mockito.any()))
                .thenReturn(new PythonSpec("backtest", java.util.Map.of()));
        String json = """
                {
                  "specType": "backtest",
                  "catalogVersion": "2026-02-02",
                  "runType": "backtest",
                  "data": {
                    "symbol": "SPY",
                    "timeframe": "1d",
                    "startDate": "2022-01-01",
                    "endDate": "2023-01-01",
                    "strategyName": "Breakout"
                  },
                  "signal": {
                    "type": "ema_cross",
                    "fast": 10,
                    "slow": 5,
                    "requireCrossing": true
                  }
                }
                """;

        mockMvc.perform(post("/api/specs/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("signal.params.fast")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("must be < signal.params.slow")));
    }

    @Test
    void validatesDateRangeAndStressTests() throws Exception {
        org.mockito.Mockito.when(pythonSpecService.buildSpec(org.mockito.Mockito.any()))
                .thenReturn(new PythonSpec("dca", java.util.Map.of()));
        String json = """
                {
                  "specType": "dca",
                  "catalogVersion": "2026-02-02",
                  "runType": "dca",
                  "data": {
                    "symbol": "BTCUSD",
                    "timeframe": "1d",
                    "frequency": "weekly",
                    "amount": 250,
                    "startDate": "2023-02-01",
                    "endDate": "2023-01-01"
                  },
                  "performance": {
                    "stressTests": {
                      "enabled": true,
                      "method": "monte_carlo",
                      "seed": 1
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("data.startDate")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("performance.stressTests.nSims")));
    }
}
