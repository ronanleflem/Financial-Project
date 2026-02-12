package finance.project.api.controllers;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.PythonSpecService;
import finance.project.api.services.PythonCanonicalRunService;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.spec.DefaultSpecBuilderFactory;
import finance.project.api.spec.builders.BacktestSpecBuilder;
import finance.project.api.validation.RunRequestValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunController.class)
@Import({
        RunRequestValidator.class,
        RunValidationErrorHandler.class,
        PythonSpecService.class,
        DefaultSpecBuilderFactory.class,
        BacktestSpecBuilder.class
})
class RunControllerInvalidSpecTypeTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RunRequestService runRequestService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private PythonCanonicalRunService pythonCanonicalRunService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RunStatusService runStatusService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RunResultService runResultService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RunMetrics runMetrics;

    @Test
    void returnsInvalidSpecTypeError() throws Exception {
        String json = """
                {
                  "specType": "unknown",
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
                    "slow": 30,
                    "requireCrossing": true
                  }
                }
                """;

        mockMvc.perform(post("/api/specs/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SPEC_TYPE"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("specType")));
    }
}
