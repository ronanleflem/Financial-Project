package finance.project.api.config;

import finance.project.api.controllers.RunController;
import finance.project.api.controllers.PreviewTimeoutException;
import finance.project.api.model.ValidationErrorItem;
import finance.project.api.model.ValidationErrorResponse;
import finance.project.api.spec.InvalidSpecTypeException;
import finance.project.api.services.RunRequestNotFoundException;
import finance.project.api.validation.RunRequestValidationException;
import finance.project.api.validation.RunTechnicalValidationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RunController.class)
public class RunValidationErrorHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final Map<String, String> FIELD_OVERRIDES = Map.of(
            "signal.fast", "signal.params.fast",
            "signal.slow", "signal.params.slow"
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ValidationErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorItem)
                .toList();
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(VALIDATION_ERROR, errors));
    }

    @ExceptionHandler(RunRequestValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleRunRequestErrors(RunRequestValidationException ex) {
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(VALIDATION_ERROR, ex.getErrors()));
    }

    @ExceptionHandler(RunTechnicalValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleRunTechnicalErrors(RunTechnicalValidationException ex) {
        return ResponseEntity.badRequest().body(new ValidationErrorResponse("INVALID_REQUEST", ex.getErrors()));
    }

    @ExceptionHandler(InvalidSpecTypeException.class)
    public ResponseEntity<ValidationErrorResponse> handleInvalidSpecType(InvalidSpecTypeException ex) {
        ValidationErrorItem error = new ValidationErrorItem("specType", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse("INVALID_SPEC_TYPE", List.of(error)));
    }

    @ExceptionHandler(RunRequestNotFoundException.class)
    public ResponseEntity<ValidationErrorResponse> handleRunNotFound(RunRequestNotFoundException ex) {
        ValidationErrorItem error = new ValidationErrorItem("requestId", "Unknown requestId");
        return ResponseEntity.status(404)
                .body(new ValidationErrorResponse("NOT_FOUND", List.of(error)));
    }

    @ExceptionHandler(PreviewTimeoutException.class)
    public ResponseEntity<ValidationErrorResponse> handlePreviewTimeout(PreviewTimeoutException ex) {
        ValidationErrorItem error = new ValidationErrorItem("preview", "timeout after " + ex.getTimeoutMillis() + "ms");
        return ResponseEntity.status(504)
                .body(new ValidationErrorResponse("TIMEOUT", List.of(error)));
    }

    private ValidationErrorItem toErrorItem(FieldError error) {
        String field = normalizeFieldPath(error.getField());
        String message = Optional.ofNullable(error.getDefaultMessage()).orElse("invalid");
        return new ValidationErrorItem(field, message);
    }

    private String normalizeFieldPath(String field) {
        return FIELD_OVERRIDES.getOrDefault(field, field);
    }
}
