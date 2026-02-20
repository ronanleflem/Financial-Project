package finance.project.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.config.PythonDispatchProperties;
import finance.project.api.observability.RunMetrics;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PythonCanonicalRunServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final RunMetrics NOOP_METRICS = new RunMetrics(
            java.util.Optional.empty(),
            new finance.project.api.observability.RunAlertingService(
                    new finance.project.api.observability.RunAlertingProperties(),
                    (type, message) -> {
                    }
            )
    );

    @Test
    void forwardsRawPayloadAndMapsSuccessContract() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        String payload = """
                {"specType":"backtest","catalogVersion":"2026-02-02","runType":"backtest","data":{"symbol":"SPY"}}
                """;
        server.expect(once(), requestTo("http://python.local/runs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-Id", "corr-1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(payload))
                .andRespond(withSuccess(
                        "{\"run_id\":\"run_123\",\"status\":\"PENDING\",\"reused\":true}",
                        MediaType.APPLICATION_JSON
                ));

        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);
        var response = service.submit(payload, "corr-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("corr-1", response.getHeaders().getFirst("X-Correlation-Id"));
        JsonNode body = (JsonNode) response.getBody();
        assertEquals("run_123", body.get("requestId").asText());
        assertEquals("PENDING", body.get("status").asText());
        assertTrue(body.get("reused").asBoolean());
        server.verify();
    }

    @Test
    void keepsAcceptedStatusFromPythonOnSubmit() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        String payload = "{\"specType\":\"backtest\"}";
        server.expect(once(), requestTo("http://python.local/runs"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"run_id\":\"run_202\",\"status\":\"QUEUED\"}"));

        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);
        var response = service.submit(payload, "corr-202");

        assertEquals(202, response.getStatusCode().value());
        JsonNode body = (JsonNode) response.getBody();
        assertEquals("run_202", body.get("requestId").asText());
        assertEquals("QUEUED", body.get("status").asText());
        server.verify();
    }

    @Test
    void keepsPython422Body() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        String payload = "{\"specType\":\"backtest\"}";
        String errorBody = "{\"run_id\":\"run_py\",\"errors\":[{\"field\":\"signal.fast\",\"code\":\"INVALID\",\"message\":\"must be >= 1\"}]}";
        server.expect(once(), requestTo("http://python.local/runs"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);
        var response = service.submit(payload, "corr-422");

        assertEquals(422, response.getStatusCode().value());
        JsonNode body = (JsonNode) response.getBody();
        assertEquals("run_py", body.get("run_id").asText());
        org.junit.jupiter.api.Assertions.assertNull(body.get("requestId"));
        assertEquals("signal.fast", body.get("errors").get(0).get("field").asText());
        assertEquals("INVALID", body.get("errors").get(0).get("code").asText());
        server.verify();
    }

    @Test
    void returnsGatewayTimeoutOnNetworkTimeout() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.any(java.net.URI.class),
                org.mockito.ArgumentMatchers.eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(String.class)
        )).thenThrow(new ResourceAccessException("read timed out", new SocketTimeoutException("Read timed out")));

        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);

        var response = service.submit("{\"specType\":\"backtest\"}", "corr-timeout");

        assertEquals(504, response.getStatusCode().value());
        JsonNode body = MAPPER.valueToTree(response.getBody());
        assertEquals("PYTHON_TIMEOUT", body.get("code").asText());
        assertEquals("Python upstream timeout", body.get("message").asText());
        assertEquals("POST /runs", body.get("endpoint").asText());
    }

    @Test
    void returnsBadGatewayWhenPythonUnavailable() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.any(java.net.URI.class),
                org.mockito.ArgumentMatchers.eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(String.class)
        )).thenThrow(new ResourceAccessException("connect failed", new ConnectException("Connection refused")));

        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);

        var response = service.submit("{\"specType\":\"backtest\"}", "corr-unavailable");

        assertEquals(502, response.getStatusCode().value());
        JsonNode body = MAPPER.valueToTree(response.getBody());
        assertEquals("PYTHON_UNAVAILABLE", body.get("code").asText());
        assertEquals("Python upstream unavailable", body.get("message").asText());
        assertEquals("POST /runs", body.get("endpoint").asText());
    }

    @Test
    void mapsRunIdOnStatusAndResultAndCancel() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);

        server.expect(once(), requestTo("http://python.local/runs/run_1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Correlation-Id", "corr-status"))
                .andRespond(withSuccess("{\"run_id\":\"run_1\",\"status\":\"RUNNING\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://python.local/runs/run_1/result"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Correlation-Id", "corr-result"))
                .andRespond(withSuccess("{\"run_id\":\"run_1\",\"status\":\"COMPLETED\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://python.local/runs/run_1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-Id", "corr-cancel"))
                .andRespond(withSuccess("{\"run_id\":\"run_1\",\"status\":\"CANCELLING\"}", MediaType.APPLICATION_JSON));

        var statusResponse = service.getStatus("run_1", "corr-status");
        JsonNode statusBody = (JsonNode) statusResponse.getBody();
        assertEquals("run_1", statusBody.get("requestId").asText());
        assertEquals("RUNNING", statusBody.get("status").asText());

        var resultResponse = service.getResult("run_1", "corr-result");
        JsonNode resultBody = (JsonNode) resultResponse.getBody();
        assertEquals("run_1", resultBody.get("requestId").asText());
        assertEquals("COMPLETED", resultBody.get("status").asText());

        var cancelResponse = service.cancel("run_1", "corr-cancel");
        JsonNode cancelBody = (JsonNode) cancelResponse.getBody();
        assertEquals("run_1", cancelBody.get("requestId").asText());
        assertEquals("CANCELLING", cancelBody.get("status").asText());

        server.verify();
    }

    @Test
    void preservesUpstreamHttpCodes() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);

        server.expect(once(), requestTo("http://python.local/runs/missing"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"NOT_FOUND\"}"));
        server.expect(once(), requestTo("http://python.local/runs/run_2/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"ALREADY_TERMINAL\"}"));
        server.expect(once(), requestTo("http://python.local/runs/run_2/result"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errors\":[{\"field\":\"requestId\",\"code\":\"INVALID\"}]}"));

        var notFound = service.getStatus("missing", "corr-404");
        var conflict = service.cancel("run_2", "corr-409");
        var validation = service.getResult("run_2", "corr-422");

        assertEquals(404, notFound.getStatusCode().value());
        assertEquals(409, conflict.getStatusCode().value());
        assertEquals(422, validation.getStatusCode().value());
        server.verify();
    }

    @Test
    void generatesAndPropagatesCorrelationIdWhenMissing() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        String payload = "{\"specType\":\"backtest\"}";
        server.expect(once(), requestTo("http://python.local/runs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String correlationId = request.getHeaders().getFirst("X-Correlation-Id");
                    assertTrue(correlationId != null && !correlationId.isBlank());
                })
                .andRespond(withSuccess("{\"run_id\":\"run_abc\",\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));

        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);
        var response = service.submit(payload, null);

        assertEquals(200, response.getStatusCode().value());
        String propagated = response.getHeaders().getFirst("X-Correlation-Id");
        assertTrue(propagated != null && !propagated.isBlank());
        JsonNode body = (JsonNode) response.getBody();
        assertEquals("run_abc", body.get("requestId").asText());
        server.verify();
    }

    @Test
    void forwardsCapabilitiesQueryParamWithoutTransformingSpecType() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(once(), requestTo("http://python.local/runs/capabilities?spec_type=dca"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Correlation-Id", "corr-cap"))
                .andRespond(withSuccess(
                        "{\"spec_type\":\"dca\",\"catalog_version\":\"2026-02-02\"}",
                        MediaType.APPLICATION_JSON
                ));

        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);
        var response = service.getCapabilities("dca", "corr-cap");

        assertEquals(200, response.getStatusCode().value());
        JsonNode body = (JsonNode) response.getBody();
        assertEquals("dca", body.get("spec_type").asText());
        assertEquals("2026-02-02", body.get("catalog_version").asText());
        server.verify();
    }

    @Test
    void keepsLegacyDcaPayloadAndArraysOnCapabilitiesProxy() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        String payload = """
                {
                  "spec_type":"dca",
                  "fields":{"supported":["entryPrice","frequency"],"accepted_but_not_wired":["slippage"]},
                  "presets":{"supported":{"safe":{"mode":"conservative"}},"not_supported":{}},
                  "legacy_dca":{"fields":{"supported_in_legacy_runner":["legacyGridStep","legacySafetyOrder"],"canonical_passthrough_supported":["legacySafetyOrder"]}}
                }
                """;

        server.expect(once(), requestTo("http://python.local/runs/capabilities?spec_type=dca"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        PythonCanonicalRunService service = new PythonCanonicalRunService(restTemplate, props, MAPPER, NOOP_METRICS);
        var response = service.getCapabilities("dca", "corr-cap-legacy");

        assertEquals(200, response.getStatusCode().value());
        JsonNode body = (JsonNode) response.getBody();
        assertEquals("dca", body.get("spec_type").asText());
        assertEquals(2, body.get("fields").get("supported").size());
        assertEquals(1, body.get("fields").get("accepted_but_not_wired").size());
        assertEquals("legacyGridStep", body.get("legacy_dca").get("fields").get("supported_in_legacy_runner").get(0).asText());
        assertEquals("legacySafetyOrder", body.get("legacy_dca").get("fields").get("supported_in_legacy_runner").get(1).asText());
        assertEquals("legacySafetyOrder", body.get("legacy_dca").get("fields").get("canonical_passthrough_supported").get(0).asText());
        server.verify();
    }
}
