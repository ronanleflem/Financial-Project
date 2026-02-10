package finance.project.api.controllers;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.model.RunResultResponse;
import finance.project.api.model.RunStatusResponse;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.PythonSpecService;
import finance.project.api.services.RunRequestNotFoundException;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.validation.RunRequestValidator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunController.class)
@Import({RunRequestValidator.class, RunValidationErrorHandler.class})
class RunControllerRunResultTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    void returnsCompletedResult() throws Exception {
        ObjectNode result = MAPPER.createObjectNode().put("metric", 1);
        RunResultResponse response = new RunResultResponse(
                "run_1",
                RunStatusResponse.Status.COMPLETED,
                result,
                List.of(new RunResultResponse.Artifact("parquet", "/tmp/a.parquet", null)),
                List.of("warn"),
                null,
                null
        );
        org.mockito.Mockito.when(runResultService.getResult("run_1")).thenReturn(response);

        mockMvc.perform(get("/api/runs/run_1/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", is("run_1")))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.result.metric", is(1)))
                .andExpect(jsonPath("$.artifacts[0].type", is("parquet")))
                .andExpect(jsonPath("$.warnings[0]", is("warn")));
    }

    @Test
    void returnsRunningWhenNotAvailable() throws Exception {
        RunResultResponse response = new RunResultResponse(
                "run_2",
                RunStatusResponse.Status.RUNNING,
                null,
                null,
                null,
                null,
                "Result not available yet"
        );
        org.mockito.Mockito.when(runResultService.getResult("run_2")).thenReturn(response);

        mockMvc.perform(get("/api/runs/run_2/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", is("run_2")))
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.message", is("Result not available yet")));
    }

    @Test
    void returnsNotFoundWhenUnknownRequestId() throws Exception {
        org.mockito.Mockito.when(runResultService.getResult("missing"))
                .thenThrow(new RunRequestNotFoundException("missing"));

        mockMvc.perform(get("/api/runs/missing/result"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.errors[0].field", is("requestId")))
                .andExpect(jsonPath("$.errors[0].message", is("Unknown requestId")));
    }
}
