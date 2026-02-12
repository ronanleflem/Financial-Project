package finance.project.api.controllers;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class RunControllerLifecyclePythonCanonicalTest {

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
    void statusHappyPath() throws Exception {
        ResponseEntity<?> statusResponse = ResponseEntity.ok()
                .header("X-Correlation-Id", "corr-status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("requestId", "run_1", "status", "RUNNING"));
        org.mockito.Mockito.doReturn(statusResponse).when(pythonCanonicalRunService).getStatus("run_1", "corr-status");

        mockMvc.perform(get("/api/runs/run_1")
                        .header("X-Correlation-Id", "corr-status"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "corr-status"))
                .andExpect(jsonPath("$.requestId", is("run_1")))
                .andExpect(jsonPath("$.status", is("RUNNING")));
    }

    @Test
    void resultTerminalAndNonTerminal() throws Exception {
        ResponseEntity<?> doneResponse = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("requestId", "run_done", "status", "COMPLETED", "result", java.util.Map.of("pnl", 12.5)));
        ResponseEntity<?> pendingResponse = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("requestId", "run_pending", "status", "RUNNING", "message", "Result not available yet"));
        org.mockito.Mockito.doReturn(doneResponse).when(pythonCanonicalRunService).getResult("run_done", "corr-res");
        org.mockito.Mockito.doReturn(pendingResponse).when(pythonCanonicalRunService).getResult("run_pending", "corr-res");

        mockMvc.perform(get("/api/runs/run_done/result")
                        .header("X-Correlation-Id", "corr-res"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.result.pnl", is(12.5)));

        mockMvc.perform(get("/api/runs/run_pending/result")
                        .header("X-Correlation-Id", "corr-res"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.message", is("Result not available yet")));
    }

    @Test
    void cancelIsIdempotent() throws Exception {
        ResponseEntity<?> cancelResponse = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("requestId", "run_cancel", "status", "CANCELLING"));
        org.mockito.Mockito.doReturn(cancelResponse).when(pythonCanonicalRunService).cancel(
                org.mockito.ArgumentMatchers.eq("run_cancel"),
                org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/api/runs/run_cancel/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLING")));

        mockMvc.perform(post("/api/runs/run_cancel/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLING")));
    }

    @Test
    void forwardsUpstreamErrors() throws Exception {
        ResponseEntity<?> notFoundResponse = ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("code", "NOT_FOUND", "message", "unknown run"));
        ResponseEntity<?> conflictResponse = ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("code", "ALREADY_TERMINAL", "message", "cannot cancel"));
        ResponseEntity<?> validationResponse = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("errors", java.util.List.of(
                        java.util.Map.of("field", "requestId", "code", "INVALID", "message", "bad format")
                )));
        ResponseEntity<?> unavailableResponse = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("code", "PYTHON_UNAVAILABLE"));
        org.mockito.Mockito.doReturn(notFoundResponse).when(pythonCanonicalRunService).getStatus("missing", "corr-404");
        org.mockito.Mockito.doReturn(conflictResponse).when(pythonCanonicalRunService).cancel("run_done", "corr-409");
        org.mockito.Mockito.doReturn(validationResponse).when(pythonCanonicalRunService).getResult("run_bad", "corr-422");
        org.mockito.Mockito.doReturn(unavailableResponse).when(pythonCanonicalRunService).getStatus("run_up", "corr-503");

        mockMvc.perform(get("/api/runs/missing")
                        .header("X-Correlation-Id", "corr-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));

        mockMvc.perform(post("/api/runs/run_done/cancel")
                        .header("X-Correlation-Id", "corr-409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ALREADY_TERMINAL")));

        mockMvc.perform(get("/api/runs/run_bad/result")
                        .header("X-Correlation-Id", "corr-422"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field", is("requestId")));

        mockMvc.perform(get("/api/runs/run_up")
                        .header("X-Correlation-Id", "corr-503"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("PYTHON_UNAVAILABLE")));
    }
}
