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
        org.mockito.Mockito.verify(canonicalRunAuditService).recordSubmit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("corr-1"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any()
        );
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
    void returnsUnsupportedFieldAsNotImplementedYetFromPython() throws Exception {
        ResponseEntity<?> response = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of(
                        "errors", java.util.List.of(
                                java.util.Map.of(
                                        "field", "strategy.tpSl.dynamicSl.mode",
                                        "code", "UNSUPPORTED_FIELD",
                                        "message", "Not implemented yet"
                                )
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
                  "strategy": {
                    "name": "Breakout",
                    "tpSl": {
                      "dynamicSl": {
                        "enabled": true,
                        "mode": "atr_trailing"
                      }
                    }
                  },
                  "signal": {"type":"ema_cross","fast":10,"slow":30,"requireCrossing":true}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field", is("strategy.tpSl.dynamicSl.mode")))
                .andExpect(jsonPath("$.errors[0].code", is("UNSUPPORTED_FIELD")))
                .andExpect(jsonPath("$.errors[0].message", is("Not implemented yet")));
    }

    @Test
    void doesNotApplyBusinessFieldValidationInCanonicalMode() throws Exception {
        ResponseEntity<?> response = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of(
                        "errors", java.util.List.of(
                                java.util.Map.of("field", "signal.fast", "code", "INVALID", "message", "must be < signal.slow")
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
                  "signal": {"type":"ema_cross","fast":10,"slow":5,"requireCrossing":true}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field", is("signal.fast")))
                .andExpect(jsonPath("$.errors[0].code", is("INVALID")))
                .andExpect(jsonPath("$.errors[0].message", is("must be < signal.slow")));

        org.mockito.Mockito.verify(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        org.mockito.Mockito.verifyNoInteractions(runRequestService);
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
        org.mockito.Mockito.verifyNoInteractions(canonicalRunAuditService);
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
        org.mockito.Mockito.verifyNoInteractions(canonicalRunAuditService);
    }

    @Test
    void returnsTechnicalErrorWhenPayloadIsNotJsonObject() throws Exception {
        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.errors[0].field", is("request")))
                .andExpect(jsonPath("$.errors[0].message", is("payload must be a JSON object")));

        org.mockito.Mockito.verifyNoInteractions(pythonCanonicalRunService);
        org.mockito.Mockito.verifyNoInteractions(canonicalRunAuditService);
    }

    @Test
    void returnsTechnicalErrorWhenPayloadIsTooLarge() throws Exception {
        String oversized = "{\"payload\":\"" + "a".repeat(1_000_001) + "\"}";

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversized))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.errors[0].field", is("request")))
                .andExpect(jsonPath("$.errors[0].message", is("payload too large")));

        org.mockito.Mockito.verifyNoInteractions(pythonCanonicalRunService);
        org.mockito.Mockito.verifyNoInteractions(canonicalRunAuditService);
    }

    @Test
    void forwardsUnknownFieldsToPythonInCanonicalMode() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("requestId", "run_unknown", "status", "PENDING"));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        String json = """
                {
                  "specType": "backtest",
                  "catalogVersion": "2026-02-02",
                  "runType": "backtest",
                  "data": {"symbol":"SPY"},
                  "experimentalOption": {"enabled": true},
                  "signal": {"type":"ema_cross","fast":10,"slow":30,"requireCrossing":true}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", is("run_unknown")));

        org.mockito.Mockito.verify(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.eq(json),
                org.mockito.ArgumentMatchers.anyString()
        );
        org.mockito.Mockito.verifyNoInteractions(runRequestService);
    }

    @Test
    void forwardsBacktestAutoSourceModeWithoutFiltering() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("request_id", "run_bt_auto", "status", "PENDING"));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        String json = """
                {
                  "spec_type": "backtest",
                  "catalog_version": "2026-02-02",
                  "run_type": "backtest",
                  "runtime_rules": {
                    "data_source_resolution": {
                      "mode": "auto"
                    }
                  },
                  "data": {"symbol":"SPY","timeframe":"1d"}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id", is("run_bt_auto")))
                .andExpect(jsonPath("$.status", is("PENDING")));

        org.mockito.Mockito.verify(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.eq(json),
                org.mockito.ArgumentMatchers.anyString()
        );
        org.mockito.Mockito.verifyNoInteractions(runRequestService);
    }

    @Test
    void preservesNoSourceAvailableErrorFromPython() throws Exception {
        ResponseEntity<?> response = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of(
                        "errors", java.util.List.of(
                                java.util.Map.of(
                                        "field", "runtime_rules.data_source_resolution",
                                        "code", "NO_SOURCE_AVAILABLE",
                                        "message", "no source available for symbol/timeframe/date range"
                                )
                        )
                ));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        String json = """
                {
                  "spec_type": "backtest",
                  "runtime_rules": {"data_source_resolution": {"mode": "auto"}},
                  "data": {"symbol":"UNKNOWN","timeframe":"1d"}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field", is("runtime_rules.data_source_resolution")))
                .andExpect(jsonPath("$.errors[0].code", is("NO_SOURCE_AVAILABLE")))
                .andExpect(jsonPath("$.errors[0].message", is("no source available for symbol/timeframe/date range")));
    }

    @Test
    void acceptsSymbolsOnlyPayloadForMarketStatsAndReturnsQueued() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("request_id", "run_ms_symbols", "status", "QUEUED"));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        String json = """
                {
                  "spec_type": "market_stats",
                  "data": {"symbols":["SPY","QQQ"],"timeframe":"1d"}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id", is("run_ms_symbols")))
                .andExpect(jsonPath("$.status", is("QUEUED")));

        org.mockito.Mockito.verify(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.eq(json),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void acceptsSymbolsOnlyPayloadForSeasonalityAndReturnsQueued() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("request_id", "run_seas_symbols", "status", "QUEUED"));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        String json = """
                {
                  "spec_type": "seasonality",
                  "data": {"symbols":["AAPL","MSFT"],"timezone":"UTC"}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id", is("run_seas_symbols")))
                .andExpect(jsonPath("$.status", is("QUEUED")));

        org.mockito.Mockito.verify(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.eq(json),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void preservesPython422BodyForInvalidSymbolsPayload() throws Exception {
        ResponseEntity<?> response = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of(
                        "errors", java.util.List.of(
                                java.util.Map.of("field", "data.symbols", "code", "INVALID", "message", "must not be empty")
                        )
                ));
        org.mockito.Mockito.doReturn(response).when(pythonCanonicalRunService).submit(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        String json = """
                {
                  "spec_type": "market_stats",
                  "data": {"symbols":[]}
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field", is("data.symbols")))
                .andExpect(jsonPath("$.errors[0].code", is("INVALID")))
                .andExpect(jsonPath("$.errors[0].message", is("must not be empty")));
    }
}
