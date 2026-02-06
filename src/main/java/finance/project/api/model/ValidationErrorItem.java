package finance.project.api.model;

public record ValidationErrorItem(
        String field,
        String message
) {
}
