package finance.project.api.controllers;

import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.CanonicalRunAuditService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RunController.class, CanonicalPreviewDisabledController.class})
@Import({RunRequestValidator.class, RunValidationErrorHandler.class})
class RunControllerDefaultModeTest {

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
    void defaultModeIsPythonCanonical() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok(java.util.Map.of("requestId", "run_default", "status", "PENDING"));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"specType\":\"backtest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", is("run_default")));

        org.mockito.Mockito.verifyNoInteractions(runRequestService);
        org.mockito.Mockito.verify(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void previewIsDisabledByDefaultInCanonicalMode() throws Exception {
        mockMvc.perform(post("/api/specs/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"specType\":\"backtest\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code", is("LEGACY_PREVIEW_DISABLED")));
    }
}
