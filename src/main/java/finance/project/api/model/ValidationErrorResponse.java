package finance.project.api.model;

import java.util.List;

public record ValidationErrorResponse(
        String code,
        List<ValidationErrorItem> errors
) {
}
