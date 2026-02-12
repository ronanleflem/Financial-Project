package finance.project.api.validation;

import finance.project.api.model.ValidationErrorItem;
import java.util.List;

public class RunTechnicalValidationException extends RuntimeException {
    private final List<ValidationErrorItem> errors;

    public RunTechnicalValidationException(List<ValidationErrorItem> errors) {
        super("Invalid technical run request");
        this.errors = errors;
    }

    public List<ValidationErrorItem> getErrors() {
        return errors;
    }
}
