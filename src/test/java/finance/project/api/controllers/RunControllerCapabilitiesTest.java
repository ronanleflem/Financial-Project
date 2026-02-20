package finance.project.api.controllers;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = RunController.class, properties = "run.engine.mode=PYTHON_CANONICAL")
@Import({RunRequestValidator.class, RunValidationErrorHandler.class})
class RunControllerCapabilitiesTest {

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
    void returnsCapabilitiesFromPythonForSpecTypeDca() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok()
                .header("X-Correlation-Id", "corr-cap")
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of(
                        "spec_type", "dca",
                        "catalog_version", "2026-02-02",
                        "fields", java.util.Map.of(
                                "supported", java.util.List.of("entryPrice", "frequency"),
                                "accepted_but_not_wired", java.util.List.of("slippage")
                        ),
                        "presets", java.util.Map.of(
                                "supported", java.util.Map.of("safe", java.util.Map.of("mode", "conservative")),
                                "not_supported", java.util.Map.of("aggressive", java.util.Map.of("reason", "not_wired"))
                        ),
                        "legacy_dca", java.util.Map.of(
                                "fields", java.util.Map.of(
                                        "supported_in_legacy_runner", java.util.List.of("legacyGridStep", "legacySafetyOrder"),
                                        "canonical_passthrough_supported", java.util.List.of("legacySafetyOrder")
                                )
                        )
                ));

        org.mockito.Mockito.when(pythonCanonicalRunService.getCapabilities("dca", "corr-cap"))
                .thenReturn(response);

        mockMvc.perform(get("/api/runs/capabilities")
                        .param("spec_type", "dca")
                        .header("X-Correlation-Id", "corr-cap"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "corr-cap"))
                .andExpect(jsonPath("$.spec_type", is("dca")))
                .andExpect(jsonPath("$.catalog_version", is("2026-02-02")))
                .andExpect(jsonPath("$.fields.supported", hasSize(2)))
                .andExpect(jsonPath("$.fields.supported[0]", is("entryPrice")))
                .andExpect(jsonPath("$.fields.accepted_but_not_wired", hasSize(1)))
                .andExpect(jsonPath("$.presets.supported.safe.mode", is("conservative")))
                .andExpect(jsonPath("$.legacy_dca.fields.supported_in_legacy_runner", hasSize(2)))
                .andExpect(jsonPath("$.legacy_dca.fields.supported_in_legacy_runner[0]", is("legacyGridStep")))
                .andExpect(jsonPath("$.legacy_dca.fields.canonical_passthrough_supported", hasSize(1)))
                .andExpect(jsonPath("$.legacy_dca.fields.canonical_passthrough_supported[0]", is("legacySafetyOrder")));

        org.mockito.Mockito.verify(pythonCanonicalRunService).getCapabilities("dca", "corr-cap");
    }

    @Test
    void returnsUnprocessableEntityWhenSpecTypeMissing() throws Exception {
        mockMvc.perform(get("/api/runs/capabilities"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.errors[0].field", is("spec_type")));

        org.mockito.Mockito.verifyNoInteractions(pythonCanonicalRunService);
    }

    @Test
    void mapsPythonTimeoutToServiceUnavailable() throws Exception {
        ResponseEntity<?> response = ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of(
                        "code", "PYTHON_TIMEOUT",
                        "message", "Python upstream timeout",
                        "endpoint", "GET /runs/capabilities"
                ));
        org.mockito.Mockito.when(pythonCanonicalRunService.getCapabilities(org.mockito.ArgumentMatchers.eq("dca"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(response);

        mockMvc.perform(get("/api/runs/capabilities").param("spec_type", "dca"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("PYTHON_TIMEOUT")));
    }

    @Test
    void forwardsExactSpecTypeQueryParam() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("spec_type", "market_stats"));
        org.mockito.Mockito.when(pythonCanonicalRunService.getCapabilities(org.mockito.ArgumentMatchers.eq("market_stats"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(response);

        mockMvc.perform(get("/api/runs/capabilities").param("spec_type", "market_stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spec_type", is("market_stats")));

        org.mockito.Mockito.verify(pythonCanonicalRunService)
                .getCapabilities(org.mockito.ArgumentMatchers.eq("market_stats"), org.mockito.ArgumentMatchers.anyString());
    }
}
