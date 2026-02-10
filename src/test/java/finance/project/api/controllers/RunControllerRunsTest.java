package finance.project.api.controllers;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.model.RunSubmitResponse;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.PythonSpecService;
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
class RunControllerRunsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PythonSpecService pythonSpecService;

    @MockBean
    private RunRequestService runRequestService;

    @MockBean
    private RunStatusService runStatusService;

    @MockBean
    private RunResultService runResultService;

    @MockBean
    private RunMetrics runMetrics;

    @Test
    void returnsRequestIdAndPending() throws Exception {
        org.mockito.Mockito.when(runRequestService.submit(org.mockito.Mockito.any()))
                .thenReturn(new RunSubmitResponse("run_20260209_001", RunLifecycleStatus.PENDING));

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
                    "slow": 30,
                    "requireCrossing": true
                  }
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", is("run_20260209_001")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void returnsDispatchFailedWhenDispatcherFails() throws Exception {
        org.mockito.Mockito.when(runRequestService.submit(org.mockito.Mockito.any()))
                .thenReturn(new RunSubmitResponse("run_20260209_002", RunLifecycleStatus.DISPATCH_FAILED));

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
                    "slow": 30,
                    "requireCrossing": true
                  }
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", is("run_20260209_002")))
                .andExpect(jsonPath("$.status", is("DISPATCH_FAILED")));
    }
}
