package finance.project.api.spec;

public class InvalidSpecTypeException extends RuntimeException {
    public InvalidSpecTypeException(String specType) {
        super("Unsupported specType: " + specType);
    }
}
