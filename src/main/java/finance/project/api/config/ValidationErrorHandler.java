package finance.project.api.config;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ValidationErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorPayload)
                .toList();
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    private Map<String, String> toErrorPayload(FieldError error) {
        return Map.of(
                "field", error.getField(),
                "defaultMessage", error.getDefaultMessage()
        );
    }
}
