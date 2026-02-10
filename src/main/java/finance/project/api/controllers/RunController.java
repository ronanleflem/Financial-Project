package finance.project.api.controllers;

import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.PreviewResponse;
import finance.project.api.model.RunResultResponse;
import finance.project.api.model.RunSubmitResponse;
import finance.project.api.model.ValidationErrorItem;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.validation.RunRequestValidationException;
import finance.project.api.validation.RunRequestValidator;
import finance.project.api.validation.RunLimitsProperties;
import finance.project.api.services.PythonSpecService;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RunController {
    private final RunRequestValidator runRequestValidator;
    private final PythonSpecService pythonSpecService;
    private final RunRequestService runRequestService;
    private final RunStatusService runStatusService;
    private final RunResultService runResultService;
    private final RunMetrics runMetrics;
    private final RunLimitsProperties limits;

    public RunController(RunRequestValidator runRequestValidator,
                         PythonSpecService pythonSpecService,
                         RunRequestService runRequestService,
                         RunStatusService runStatusService,
                         RunResultService runResultService,
                         RunMetrics runMetrics,
                         Optional<RunLimitsProperties> limits) {
        this.runRequestValidator = runRequestValidator;
        this.pythonSpecService = pythonSpecService;
        this.runRequestService = runRequestService;
        this.runStatusService = runStatusService;
        this.runResultService = runResultService;
        this.runMetrics = runMetrics;
        this.limits = limits.orElse(new RunLimitsProperties());
    }

    @PostMapping("/runs")
    public ResponseEntity<RunSubmitResponse> submit(@Valid @RequestBody RunRequestInput input) {
        if (input != null) {
            MDC.put("specType", input.specType());
        }
        validate(input);
        return ResponseEntity.ok(runRequestService.submit(input));
    }

    @org.springframework.web.bind.annotation.GetMapping("/runs/{requestId}")
    public ResponseEntity<finance.project.api.model.RunStatusResponse> status(
            @org.springframework.web.bind.annotation.PathVariable String requestId
    ) {
        long startNs = System.nanoTime();
        try {
            return ResponseEntity.ok(runStatusService.getStatus(requestId));
        } finally {
            runMetrics.recordStatusLatencyMillis((System.nanoTime() - startNs) / 1_000_000);
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/runs/{requestId}/result")
    public ResponseEntity<RunResultResponse> result(
            @org.springframework.web.bind.annotation.PathVariable String requestId
    ) {
        long startNs = System.nanoTime();
        try {
            return ResponseEntity.ok(runResultService.getResult(requestId));
        } finally {
            runMetrics.recordStatusLatencyMillis((System.nanoTime() - startNs) / 1_000_000);
        }
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
}
