package finance.project.api.controllers;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.PythonCanonicalRunService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = RunController.class, properties = "run.engine.mode=PYTHON_CANONICAL")
@Import({RunRequestValidator.class, RunValidationErrorHandler.class})
class RunControllerRunsPythonCanonicalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PythonSpecService pythonSpecService;

    @MockBean
    private PythonCanonicalRunService pythonCanonicalRunService;

    @MockBean
    private RunRequestService runRequestService;

    @MockBean
    private RunStatusService runStatusService;

    @MockBean
    private RunResultService runResultService;

    @MockBean
    private RunMetrics runMetrics;

    @Test
    void forwardsToPythonCanonicalAndReturnsMappedBody() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok()
                .header("X-Correlation-Id", "corr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("requestId", "run_123", "status", "PENDING", "reused", true));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("corr-1"));

        String json = """
                {
                  "specType": "backtest",
                  "data": {"symbol":"SPY"}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .header("X-Correlation-Id", "corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "corr-1"))
                .andExpect(jsonPath("$.requestId", is("run_123")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.reused", is(true)));

        org.mockito.Mockito.verifyNoInteractions(runRequestService);
    }

    @Test
    void returnsUnprocessableEntityFromPython() throws Exception {
        ResponseEntity<?> response = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of(
                        "errors", java.util.List.of(
                                java.util.Map.of("field", "signal.fast", "code", "INVALID", "message", "must be >= 1")
                        )
                ));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        String json = """
                {
                  "specType": "backtest",
                  "catalogVersion": "2026-02-02",
                  "runType": "backtest",
                  "data": {"symbol":"SPY"},
                  "signal": {"type":"ema_cross","fast":0,"slow":2,"requireCrossing":true}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field", is("signal.fast")))
                .andExpect(jsonPath("$.errors[0].code", is("INVALID")));
    }

    @Test
    void returnsGatewayTimeoutWhenPythonUnavailable() throws Exception {
        ResponseEntity<?> response = ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("code", "PYTHON_TIMEOUT", "message", "Timeout while calling Python POST /runs"));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"specType\":\"backtest\"}"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code", is("PYTHON_TIMEOUT")));
    }

    @Test
    void generatesCorrelationIdWhenHeaderMissing() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok(java.util.Map.of("requestId", "run_321", "status", "PENDING"));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"specType\":\"backtest\"}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.argThat(value -> value != null && !value.isBlank())
        );
    }

    @Test
    void returnsTechnicalErrorWhenPayloadIsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"specType\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.errors[0].field", is("request")));

        org.mockito.Mockito.verifyNoInteractions(pythonCanonicalRunService);
    }

    @Test
    void returnsTechnicalErrorWhenPayloadIsEmpty() throws Exception {
        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(" "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.errors[0].field", is("request")));

        org.mockito.Mockito.verifyNoInteractions(pythonCanonicalRunService);
    }
}
