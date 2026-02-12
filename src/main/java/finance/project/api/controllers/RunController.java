package finance.project.api.controllers;

import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.PreviewResponse;
import finance.project.api.model.ValidationErrorItem;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.services.PythonCanonicalRunService;
import finance.project.api.validation.RunRequestValidationException;
import finance.project.api.validation.RunRequestValidator;
import finance.project.api.validation.RunLimitsProperties;
import finance.project.api.services.PythonSpecService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RunController {
    private final RunRequestValidator runRequestValidator;
    private final PythonSpecService pythonSpecService;
    private final PythonCanonicalRunService pythonCanonicalRunService;
    private final RunRequestService runRequestService;
    private final RunStatusService runStatusService;
    private final RunResultService runResultService;
    private final RunMetrics runMetrics;
    private final RunLimitsProperties limits;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final String runEngineMode;

    public RunController(RunRequestValidator runRequestValidator,
                         PythonSpecService pythonSpecService,
                         PythonCanonicalRunService pythonCanonicalRunService,
                         RunRequestService runRequestService,
                         RunStatusService runStatusService,
                         RunResultService runResultService,
                         RunMetrics runMetrics,
                         Optional<RunLimitsProperties> limits,
                         ObjectMapper objectMapper,
                         Validator validator,
                         @Value("${run.engine.mode:LEGACY}") String runEngineMode) {
        this.runRequestValidator = runRequestValidator;
        this.pythonSpecService = pythonSpecService;
        this.pythonCanonicalRunService = pythonCanonicalRunService;
        this.runRequestService = runRequestService;
        this.runStatusService = runStatusService;
        this.runResultService = runResultService;
        this.runMetrics = runMetrics;
        this.limits = limits.orElse(new RunLimitsProperties());
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.runEngineMode = runEngineMode;
    }

    @PostMapping("/runs")
    public ResponseEntity<?> submit(@RequestBody String rawPayload,
                                    @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader) {
        if (isPythonCanonicalMode()) {
            String correlationId = normalizeCorrelationId(correlationIdHeader);
            return pythonCanonicalRunService.submit(rawPayload, correlationId);
        }
        RunRequestInput input = parseRunRequestInput(rawPayload);
        if (input != null) {
            MDC.put("specType", input.specType());
        }
        validateBeanConstraints(input);
        validate(input);
        return ResponseEntity.ok(runRequestService.submit(input));
    }

    @org.springframework.web.bind.annotation.GetMapping("/runs/{requestId}")
    public ResponseEntity<?> status(
            @org.springframework.web.bind.annotation.PathVariable String requestId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader
    ) {
        long startNs = System.nanoTime();
        try {
            if (isPythonCanonicalMode()) {
                String correlationId = normalizeCorrelationId(correlationIdHeader);
                return pythonCanonicalRunService.getStatus(requestId, correlationId);
            }
            return ResponseEntity.ok(runStatusService.getStatus(requestId));
        } finally {
            runMetrics.recordStatusLatencyMillis((System.nanoTime() - startNs) / 1_000_000);
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/runs/{requestId}/result")
    public ResponseEntity<?> result(
            @org.springframework.web.bind.annotation.PathVariable String requestId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader
    ) {
        long startNs = System.nanoTime();
        try {
            if (isPythonCanonicalMode()) {
                String correlationId = normalizeCorrelationId(correlationIdHeader);
                return pythonCanonicalRunService.getResult(requestId, correlationId);
            }
            return ResponseEntity.ok(runResultService.getResult(requestId));
        } finally {
            runMetrics.recordStatusLatencyMillis((System.nanoTime() - startNs) / 1_000_000);
        }
    }

    @PostMapping("/runs/{requestId}/cancel")
    public ResponseEntity<?> cancel(
            @org.springframework.web.bind.annotation.PathVariable String requestId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader
    ) {
        String correlationId = normalizeCorrelationId(correlationIdHeader);
        if (isPythonCanonicalMode()) {
            return pythonCanonicalRunService.cancel(requestId, correlationId);
        }
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Cancel endpoint is not available in legacy mode");
    }

    @PostMapping("/specs/preview")
    public ResponseEntity<PreviewResponse> preview(@Valid @RequestBody RunRequestInput input) {
        long startNs = System.nanoTime();
        if (input != null) {
            MDC.put("specType", input.specType());
        }
        validate(input);
        try {
            PythonSpec spec = buildSpecWithTimeout(input);
            List<String> normalizedFields = detectNormalizedFields(input);
            return ResponseEntity.ok(new PreviewResponse(spec, List.of(), normalizedFields));
        } finally {
            runMetrics.recordPreviewLatencyMillis((System.nanoTime() - startNs) / 1_000_000);
        }
    }

    private PythonSpec buildSpecWithTimeout(RunRequestInput input) {
        try {
            return CompletableFuture.supplyAsync(() -> pythonSpecService.buildSpec(input))
                    .orTimeout(limits.getPreviewTimeoutMillis(), TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof TimeoutException) {
                throw new PreviewTimeoutException(limits.getPreviewTimeoutMillis());
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw ex;
        }
    }

    private void validate(RunRequestInput input) {
        List<ValidationErrorItem> errors = runRequestValidator.validate(input);
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

    private static List<String> detectNormalizedFields(RunRequestInput input) {
        if (input == null) {
            return List.of();
        }
        if (!"seasonality".equals(input.specType())) {
            return List.of();
        }
        if (input.seasonality() == null) {
            return List.of();
        }

        List<String> fields = new ArrayList<>();

        finance.project.api.model.run.SeasonalitySignal signal = input.seasonality().signal();
        if (signal != null) {
            if (signal.threshold() == null) {
                fields.add("seasonality.signal.threshold");
            }
            if (signal.topk() == null) {
                fields.add("seasonality.signal.topk");
            }
        }

        finance.project.api.model.run.SeasonalityCompute compute = input.seasonality().compute();
        if (compute == null || compute.maxTrials() == null) {
            fields.add("seasonality.compute.maxTrials");
        }
        if (compute == null || compute.searchSpace() == null || compute.searchSpace().trim().isEmpty()) {
            fields.add("seasonality.compute.searchSpace");
        }

        finance.project.api.model.run.SeasonalityExecution execution = input.seasonality().execution();
        if (execution == null || execution.riskModel() == null || execution.riskModel().trim().isEmpty()) {
            fields.add("seasonality.execution.riskModel");
        }
        if (execution == null || execution.tpSl() == null || execution.tpSl().trim().isEmpty()) {
            fields.add("seasonality.execution.tpSl");
        }

        return fields;
    }

    private RunRequestInput parseRunRequestInput(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, RunRequestInput.class);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed run payload", ex);
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
}
