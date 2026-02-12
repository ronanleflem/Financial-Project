package finance.project.api.controllers;

import finance.project.api.model.PreviewResponse;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.PythonSpecService;
import finance.project.api.validation.RunLimitsProperties;
import finance.project.api.validation.RunRequestValidationException;
import finance.project.api.validation.RunRequestValidator;
import finance.project.api.model.ValidationErrorItem;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Deprecated
@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "run.engine.mode", havingValue = "LEGACY")
public class LegacyRunSpecPreviewController {
    private final RunRequestValidator runRequestValidator;
    private final PythonSpecService pythonSpecService;
    private final RunMetrics runMetrics;
    private final RunLimitsProperties limits;

    public LegacyRunSpecPreviewController(RunRequestValidator runRequestValidator,
                                          PythonSpecService pythonSpecService,
                                          RunMetrics runMetrics,
                                          Optional<RunLimitsProperties> limits) {
        this.runRequestValidator = runRequestValidator;
        this.pythonSpecService = pythonSpecService;
        this.runMetrics = runMetrics;
        this.limits = limits.orElse(new RunLimitsProperties());
    }

    @PostMapping({"/specs/preview", "/runs/specs/preview"})
    public ResponseEntity<PreviewResponse> preview(@Valid @RequestBody RunRequestInput input) {
        long startNs = System.nanoTime();
        if (input != null) {
            MDC.put("specType", input.specType());
        }
        validate(input);
        try {
            PythonSpec spec = buildSpecWithTimeout(input);
            List<String> normalizedFields = detectNormalizedFields(input);
            return ResponseEntity.ok()
                    .header("Deprecation", "true")
                    .header("Sunset", "Tue, 30 Jun 2026 23:59:59 GMT")
                    .body(new PreviewResponse(spec, List.of(), normalizedFields));
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

    private static List<String> detectNormalizedFields(RunRequestInput input) {
        if (input == null || !"seasonality".equals(input.specType()) || input.seasonality() == null) {
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
}
