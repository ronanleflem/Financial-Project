package finance.project.api.controllers;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.model.RunStatusResponse;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.PythonSpecService;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunRequestNotFoundException;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.validation.RunRequestValidator;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunController.class)
@Import({RunRequestValidator.class, RunValidationErrorHandler.class})
class RunControllerRunStatusTest {

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
    void returnsStatusResponse() throws Exception {
        org.mockito.Mockito.when(runStatusService.getStatus("run_1"))
                .thenReturn(new RunStatusResponse("run_1", RunStatusResponse.Status.RUNNING, Instant.parse("2026-02-09T12:30:00Z"), null));

        mockMvc.perform(get("/api/runs/run_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", is("run_1")))
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.updatedAt", is("2026-02-09T12:30:00Z")));
    }

    @Test
    void returnsNotFoundWhenUnknownRequestId() throws Exception {
        org.mockito.Mockito.when(runStatusService.getStatus("missing"))
                .thenThrow(new RunRequestNotFoundException("missing"));

        mockMvc.perform(get("/api/runs/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.errors[0].field", is("requestId")))
                .andExpect(jsonPath("$.errors[0].message", is("Unknown requestId")));
    }
}
