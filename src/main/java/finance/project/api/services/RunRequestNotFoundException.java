package finance.project.api.services;

public class RunRequestNotFoundException extends RuntimeException {
    private final String requestId;

    public RunRequestNotFoundException(String requestId) {
        super("Unknown requestId");
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}

