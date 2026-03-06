package finance.project.api.controllers;

import finance.project.api.model.marketanalysis.MarketAnalysisErrorItem;
import finance.project.api.model.marketanalysis.MarketAnalysisErrorResponse;
import finance.project.api.model.marketanalysis.MarketAnalysisRunDetailResponse;
import finance.project.api.model.marketanalysis.MarketAnalysisRunListResponse;
import finance.project.api.model.marketanalysis.MarketAnalysisRunResultResponse;
import finance.project.api.services.marketanalysis.MarketAnalysisNotFoundException;
import finance.project.api.services.marketanalysis.MarketAnalysisResultNotReadyException;
import finance.project.api.services.marketanalysis.MarketAnalysisService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-analysis")
public class MarketAnalysisController {
    private static final List<String> SUPPORTED_SPEC_TYPES = List.of("market_stats", "seasonality");

    private final MarketAnalysisService marketAnalysisService;

    public MarketAnalysisController(MarketAnalysisService marketAnalysisService) {
        this.marketAnalysisService = marketAnalysisService;
    }

    @GetMapping("/runs")
    public ResponseEntity<MarketAnalysisRunListResponse> listRuns(
            @RequestParam(name = "spec_type") String specType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_at,desc") String sort
    ) {
        validateSpecType(specType);
        Instant fromInstant = parseInstantOrThrow("from", from);
        Instant toInstant = parseInstantOrThrow("to", to);
        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw invalidRequest("from", "INVALID_DATE_RANGE", "from must be <= to");
        }
        if (page < 0) {
            throw invalidRequest("page", "INVALID_PAGINATION", "page must be >= 0");
        }
        if (size < 1 || size > 200) {
            throw invalidRequest("size", "INVALID_PAGINATION", "size must be between 1 and 200");
        }

        MarketAnalysisRunListResponse response = marketAnalysisService.listRuns(
                specType, status, symbol, timeframe, fromInstant, toInstant, page, size, sort
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<MarketAnalysisRunDetailResponse> getRun(@PathVariable String runId) {
        return ResponseEntity.ok(marketAnalysisService.getRunDetail(runId));
    }

    @GetMapping("/runs/{runId}/result")
    public ResponseEntity<MarketAnalysisRunResultResponse> getRunResult(@PathVariable String runId) {
        return ResponseEntity.ok(marketAnalysisService.getRunResult(runId));
    }

    @ExceptionHandler(MarketAnalysisNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(MarketAnalysisNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "code", "RUN_NOT_FOUND",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MarketAnalysisResultNotReadyException.class)
    public ResponseEntity<Map<String, Object>> handleNotReady(MarketAnalysisResultNotReadyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "RESULT_NOT_READY",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MarketAnalysisValidationException.class)
    public ResponseEntity<MarketAnalysisErrorResponse> handleValidation(MarketAnalysisValidationException ex) {
        return ResponseEntity.unprocessableEntity().body(new MarketAnalysisErrorResponse(List.of(
                new MarketAnalysisErrorItem(ex.field(), ex.code(), ex.getMessage())
        )));
    }

    private void validateSpecType(String specType) {
        if (specType == null || specType.isBlank()) {
            throw invalidRequest("spec_type", "INVALID_SPEC_TYPE", "spec_type is required");
        }
        if (!SUPPORTED_SPEC_TYPES.contains(specType)) {
            throw invalidRequest("spec_type", "INVALID_SPEC_TYPE", "spec_type must be one of: market_stats, seasonality");
        }
    }

    private Instant parseInstantOrThrow(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw invalidRequest(field, "INVALID_DATETIME", "must be ISO-8601 instant");
        }
    }

    private MarketAnalysisValidationException invalidRequest(String field, String code, String message) {
        return new MarketAnalysisValidationException(field, code, message);
    }

    private static final class MarketAnalysisValidationException extends RuntimeException {
        private final String field;
        private final String code;

        private MarketAnalysisValidationException(String field, String code, String message) {
            super(message);
            this.field = field;
            this.code = code;
        }

        public String field() {
            return field;
        }

        public String code() {
            return code;
        }
    }
}
