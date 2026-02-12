package finance.project.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import finance.project.api.config.PythonDispatchProperties;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PythonCanonicalRunService {
    private static final Logger log = LoggerFactory.getLogger(PythonCanonicalRunService.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final RestTemplate restTemplate;
    private final PythonDispatchProperties props;
    private final ObjectMapper objectMapper;

    public PythonCanonicalRunService(RestTemplateBuilder builder,
                                     PythonDispatchProperties props,
                                     ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMillis()))
                .setReadTimeout(Duration.ofMillis(props.getReadTimeoutMillis()))
                .build();
    }

    PythonCanonicalRunService(RestTemplate restTemplate,
                              PythonDispatchProperties props,
                              ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<?> submit(String rawPayload, String correlationId) {
        return proxyRuns("POST /runs", "/runs", HttpMethod.POST, rawPayload, correlationId, true);
    }

    public ResponseEntity<?> getStatus(String requestId, String correlationId) {
        String normalizedId = normalizeRequestId(requestId);
        return proxyRuns(
                "GET /runs/{id}",
                "/runs/" + normalizedId,
                HttpMethod.GET,
                null,
                correlationId,
                true
        );
    }

    public ResponseEntity<?> getResult(String requestId, String correlationId) {
        String normalizedId = normalizeRequestId(requestId);
        return proxyRuns(
                "GET /runs/{id}/result",
                "/runs/" + normalizedId + "/result",
                HttpMethod.GET,
                null,
                correlationId,
                true
        );
    }

    public ResponseEntity<?> cancel(String requestId, String correlationId) {
        String normalizedId = normalizeRequestId(requestId);
        return proxyRuns(
                "POST /runs/{id}/cancel",
                "/runs/" + normalizedId + "/cancel",
                HttpMethod.POST,
                null,
                correlationId,
                true
        );
    }

    private ResponseEntity<?> proxyRuns(String endpoint,
                                        String upstreamPath,
                                        HttpMethod method,
                                        String rawPayload,
                                        String correlationId,
                                        boolean mapRunIdField) {
        long startNs = System.nanoTime();
        String specType = extractText(rawPayload, "specType");
        String requestId = extractRequestIdFromPath(upstreamPath);
        int httpStatus = 500;
        try {
            URI uri = URI.create(trimTrailingSlash(props.getBaseUrl()) + upstreamPath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(CORRELATION_HEADER, correlationId);
            HttpEntity<String> entity = new HttpEntity<>(rawPayload, headers);

            ResponseEntity<String> pythonResponse = restTemplate.exchange(uri, method, entity, String.class);
            httpStatus = pythonResponse.getStatusCode().value();

            Object mapped = mapSuccessBody(pythonResponse.getBody(), mapRunIdField);
            requestId = firstNonBlank(
                    requestId,
                    extractText(mapped, "requestId"),
                    extractText(mapped, "run_id")
            );
            return ResponseEntity.status(pythonResponse.getStatusCode())
                    .header(CORRELATION_HEADER, correlationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapped);
        } catch (HttpStatusCodeException ex) {
            httpStatus = ex.getStatusCode().value();
            Object errorBody = mapErrorBody(ex.getResponseBodyAsString(), mapRunIdField);
            requestId = firstNonBlank(
                    requestId,
                    extractText(errorBody, "requestId"),
                    extractText(errorBody, "run_id")
            );
            return ResponseEntity.status(ex.getStatusCode())
                    .header(CORRELATION_HEADER, correlationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody);
        } catch (ResourceAccessException ex) {
            Throwable cause = rootCause(ex);
            boolean timeout = cause instanceof SocketTimeoutException;
            boolean connectIssue = cause instanceof ConnectException;
            httpStatus = timeout ? 504 : 502;
            Map<String, Object> body = timeout
                    ? Map.of("code", "PYTHON_TIMEOUT", "message", "Timeout while calling Python " + endpoint)
                    : Map.of("code", "PYTHON_UNAVAILABLE",
                    "message", connectIssue
                            ? "Python service unavailable while calling " + endpoint
                            : "Python upstream error while calling " + endpoint);
            return ResponseEntity.status(httpStatus)
                    .header(CORRELATION_HEADER, correlationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (RestClientException ex) {
            httpStatus = 502;
            Map<String, Object> body = Map.of(
                    "code", "PYTHON_UPSTREAM_ERROR",
                    "message", "Error while calling Python " + endpoint + ": " + safeMessage(ex)
            );
            return ResponseEntity.status(httpStatus)
                    .header(CORRELATION_HEADER, correlationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } finally {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info(
                    "run_proxy requestId={} endpoint={} status={} latency_ms={} specType={}",
                    requestId,
                    endpoint,
                    httpStatus,
                    latencyMs,
                    specType
            );
        }
    }

    private Object mapSuccessBody(String body, boolean mapRunIdField) {
        JsonNode source = parseJsonNodeOrEmpty(body);
        if (source.isObject()) {
            return mapRunIdField((ObjectNode) source, mapRunIdField);
        }
        if (source.isNull() || source.isMissingNode()) {
            return objectMapper.createObjectNode();
        }
        return source;
    }

    private Object mapErrorBody(String body, boolean mapRunIdField) {
        Object parsed = parseJsonOrRaw(body);
        if (parsed instanceof ObjectNode node) {
            return mapRunIdField(node, mapRunIdField);
        }
        return parsed;
    }

    private ObjectNode mapRunIdField(ObjectNode node, boolean mapRunIdField) {
        if (!mapRunIdField) {
            return node;
        }
        if (node.has("run_id") && !node.has("requestId")) {
            node.set("requestId", node.get("run_id"));
        }
        return node;
    }

    private JsonNode parseJsonNodeOrEmpty(String body) {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private Object parseJsonOrRaw(String body) {
        if (body == null || body.isBlank()) {
            return Map.of("message", "Empty response body from Python");
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return body;
        }
    }

    private String extractText(String rawPayload, String fieldName) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            return textOrNull(root.get(fieldName));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractText(Object body, String fieldName) {
        if (body instanceof JsonNode node) {
            return textOrNull(node.get(fieldName));
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private static String normalizeRequestId(String requestId) {
        if (requestId == null) {
            return "";
        }
        return requestId.trim();
    }

    private static String extractRequestIdFromPath(String upstreamPath) {
        if (upstreamPath == null || upstreamPath.isBlank()) {
            return null;
        }
        String[] parts = upstreamPath.split("/");
        if (parts.length < 3) {
            return null;
        }
        if (!"runs".equalsIgnoreCase(parts[1])) {
            return null;
        }
        String candidate = parts[2];
        return candidate.isBlank() ? null : candidate;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
