package finance.project.api.validation;

import finance.project.api.model.ValidationErrorItem;
import java.util.List;

public class RunRequestValidationException extends RuntimeException {
    private final List<ValidationErrorItem> errors;

    public RunRequestValidationException(List<ValidationErrorItem> errors) {
        super("Run request validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<ValidationErrorItem> getErrors() {
        return errors;
    }
}
