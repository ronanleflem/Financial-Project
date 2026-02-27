package finance.project.api.controllers;

import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.StressSourceRunsResponse;
import finance.project.api.model.ValidationErrorItem;
import finance.project.api.model.ValidationErrorResponse;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.services.PythonCanonicalRunService;
import finance.project.api.services.CanonicalRunAuditService;
import finance.project.api.services.StressSourceRunService;
import finance.project.api.validation.RunRequestValidationException;
import finance.project.api.validation.RunTechnicalValidationException;
import finance.project.api.validation.RunRequestValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api")
public class RunController {
    private static final int MAX_CANONICAL_PAYLOAD_BYTES = 1_000_000;
    private final PythonCanonicalRunService pythonCanonicalRunService;
    private final CanonicalRunAuditService canonicalRunAuditService;
    private final ObjectProvider<RunRequestValidator> runRequestValidator;
    private final ObjectProvider<RunRequestService> runRequestService;
    private final ObjectProvider<RunStatusService> runStatusService;
    private final ObjectProvider<RunResultService> runResultService;
    private final ObjectProvider<StressSourceRunService> stressSourceRunService;
    private final RunMetrics runMetrics;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final String runEngineMode;

    public RunController(ObjectProvider<RunRequestValidator> runRequestValidator,
                         PythonCanonicalRunService pythonCanonicalRunService,
                         CanonicalRunAuditService canonicalRunAuditService,
                         ObjectProvider<RunRequestService> runRequestService,
                         ObjectProvider<RunStatusService> runStatusService,
                         ObjectProvider<RunResultService> runResultService,
                         ObjectProvider<StressSourceRunService> stressSourceRunService,
                         RunMetrics runMetrics,
                         ObjectMapper objectMapper,
                         Validator validator,
                         @Value("${run.engine.mode:PYTHON_CANONICAL}") String runEngineMode) {
        this.runRequestValidator = runRequestValidator;
        this.pythonCanonicalRunService = pythonCanonicalRunService;
        this.canonicalRunAuditService = canonicalRunAuditService;
        this.runRequestService = runRequestService;
        this.runStatusService = runStatusService;
        this.runResultService = runResultService;
        this.stressSourceRunService = stressSourceRunService;
        this.runMetrics = runMetrics;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.runEngineMode = runEngineMode;
    }

    @PostMapping("/runs")
    public ResponseEntity<?> submit(@RequestBody String rawPayload,
                                    @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
                                    HttpServletRequest request) {
        if (isPythonCanonicalMode()) {
            validateCanonicalTechnicalPayload(rawPayload);
            String correlationId = normalizeCorrelationId(correlationIdHeader);
            ResponseEntity<?> response = pythonCanonicalRunService.submit(rawPayload, correlationId);
            canonicalRunAuditService.recordSubmit(rawPayload, correlationId, resolveActor(request), response);
            return response;
        }
        RunRequestInput input = parseRunRequestInput(rawPayload);
        if (input != null) {
            MDC.put("specType", input.specType());
        }
        validateBeanConstraints(input);
        validate(input);
        return ResponseEntity.ok(requireLegacyBean(runRequestService, "RunRequestService").submit(input));
    }

    @org.springframework.web.bind.annotation.GetMapping("/runs/{requestId}")
    public ResponseEntity<?> status(
            @org.springframework.web.bind.annotation.PathVariable String requestId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
            HttpServletRequest request
    ) {
        long startNs = System.nanoTime();
        try {
            if (isPythonCanonicalMode()) {
                String correlationId = normalizeCorrelationId(correlationIdHeader);
                ResponseEntity<?> response = pythonCanonicalRunService.getStatus(requestId, correlationId);
                canonicalRunAuditService.recordLifecycle(requestId, correlationId, resolveActor(request), response);
                return response;
            }
            return ResponseEntity.ok(requireLegacyBean(runStatusService, "RunStatusService").getStatus(requestId));
        } finally {
            runMetrics.recordStatusLatencyMillis((System.nanoTime() - startNs) / 1_000_000);
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/runs/{requestId}/result")
    public ResponseEntity<?> result(
            @org.springframework.web.bind.annotation.PathVariable String requestId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
            HttpServletRequest request
    ) {
        long startNs = System.nanoTime();
        try {
            if (isPythonCanonicalMode()) {
                String correlationId = normalizeCorrelationId(correlationIdHeader);
                ResponseEntity<?> response = pythonCanonicalRunService.getResult(requestId, correlationId);
                canonicalRunAuditService.recordLifecycle(requestId, correlationId, resolveActor(request), response);
                return response;
            }
            return ResponseEntity.ok(requireLegacyBean(runResultService, "RunResultService").getResult(requestId));
        } finally {
            runMetrics.recordStatusLatencyMillis((System.nanoTime() - startNs) / 1_000_000);
        }
    }

    @PostMapping("/runs/{requestId}/cancel")
    public ResponseEntity<?> cancel(
            @org.springframework.web.bind.annotation.PathVariable String requestId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
            HttpServletRequest request
    ) {
        String correlationId = normalizeCorrelationId(correlationIdHeader);
        if (isPythonCanonicalMode()) {
            ResponseEntity<?> response = pythonCanonicalRunService.cancel(requestId, correlationId);
            canonicalRunAuditService.recordLifecycle(requestId, correlationId, resolveActor(request), response);
            return response;
        }
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Cancel endpoint is not available in legacy mode");
    }

    @GetMapping("/runs/capabilities")
    public ResponseEntity<?> capabilities(
            @RequestParam(value = "spec_type", required = false) String specType,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader
    ) {
        if (specType == null || specType.trim().isEmpty()) {
            ValidationErrorItem error = new ValidationErrorItem("spec_type", "must not be blank");
            return ResponseEntity.unprocessableEntity()
                    .body(new ValidationErrorResponse("INVALID_REQUEST", List.of(error)));
        }

        String correlationId = normalizeCorrelationId(correlationIdHeader);
        ResponseEntity<?> upstream = pythonCanonicalRunService.getCapabilities(specType.trim(), correlationId);

        if (upstream.getStatusCode().value() == HttpStatus.GATEWAY_TIMEOUT.value()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(upstream.getHeaders())
                    .body(upstream.getBody());
        }

        return upstream;
    }

    @Operation(summary = "List eligible source runs for stress test launch")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Eligible canonical source runs",
                    content = @Content(schema = @Schema(implementation = StressSourceRunsResponse.class))
            ),
            @ApiResponse(responseCode = "422", description = "Invalid request")
    })
    @GetMapping("/runs/stress/sources")
    public ResponseEntity<?> listStressSourceRuns(
            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(description = "Pagination cursor from previous response")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "Optional filter: dca or backtest")
            @RequestParam(value = "strategyType", required = false) String strategyType
    ) {
        if (strategyType != null && !strategyType.isBlank()) {
            String normalized = strategyType.trim().toLowerCase();
            if (!"dca".equals(normalized) && !"backtest".equals(normalized)) {
                ValidationErrorItem error = new ValidationErrorItem("strategyType", "must be one of: dca, backtest");
                return ResponseEntity.unprocessableEntity()
                        .body(new ValidationErrorResponse("INVALID_REQUEST", List.of(error)));
            }
        }
        StressSourceRunsResponse response = requireLegacyBean(stressSourceRunService, "StressSourceRunService")
                .listEligibleSources(limit, cursor, strategyType);
        return ResponseEntity.ok(response);
    }

    private void validate(RunRequestInput input) {
        List<ValidationErrorItem> errors = requireLegacyBean(runRequestValidator, "RunRequestValidator").validate(input);
        if (!errors.isEmpty()) {
            throw new RunRequestValidationException(errors);
        }
    }

    private void validateBeanConstraints(RunRequestInput input) {
        if (input == null) {
            return;
        }
        Set<ConstraintViolation<RunRequestInput>> violations = validator.validate(input);
        if (violations.isEmpty()) {
            return;
        }
        Map<String, String> fieldOverrides = Map.of(
                "signal.fast", "signal.params.fast",
                "signal.slow", "signal.params.slow"
        );
        List<ValidationErrorItem> errors = violations.stream()
                .map(violation -> {
                    String rawField = violation.getPropertyPath().toString();
                    String field = fieldOverrides.getOrDefault(rawField, rawField);
                    return new ValidationErrorItem(field, violation.getMessage());
                })
                .toList();
        throw new RunRequestValidationException(errors);
    }

    private RunRequestInput parseRunRequestInput(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, RunRequestInput.class);
        } catch (JsonProcessingException ex) {
            throw new RunTechnicalValidationException(List.of(
                    new ValidationErrorItem("request", "malformed JSON payload")
            ));
        }
    }

    private void validateCanonicalTechnicalPayload(String rawPayload) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new RunTechnicalValidationException(List.of(
                    new ValidationErrorItem("request", "payload must not be empty")
            ));
        }
        if (rawPayload.length() > MAX_CANONICAL_PAYLOAD_BYTES) {
            throw new RunTechnicalValidationException(List.of(
                    new ValidationErrorItem("request", "payload too large")
            ));
        }
        try {
            JsonNode node = objectMapper.readTree(rawPayload);
            if (node == null || !node.isObject()) {
                throw new RunTechnicalValidationException(List.of(
                        new ValidationErrorItem("request", "payload must be a JSON object")
                ));
            }
        } catch (JsonProcessingException ex) {
            throw new RunTechnicalValidationException(List.of(
                    new ValidationErrorItem("request", "malformed JSON payload")
            ));
        }
    }

    private boolean isPythonCanonicalMode() {
        return "PYTHON_CANONICAL".equalsIgnoreCase(runEngineMode);
    }

    private static String normalizeCorrelationId(String correlationIdHeader) {
        if (correlationIdHeader == null || correlationIdHeader.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return correlationIdHeader.trim();
    }

    private static String resolveActor(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String remoteUser = request.getRemoteUser();
        if (remoteUser != null && !remoteUser.trim().isEmpty()) {
            return remoteUser.trim();
        }
        String actorHeader = request.getHeader("X-Actor");
        if (actorHeader != null && !actorHeader.trim().isEmpty()) {
            return actorHeader.trim();
        }
        return null;
    }

    private static <T> T requireLegacyBean(ObjectProvider<T> provider, String beanName) {
        T bean = provider.getIfAvailable();
        if (bean == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Legacy run engine component unavailable: " + beanName
            );
        }
        return bean;
    }
}
