package finance.project.api.controllers;

public class PreviewTimeoutException extends RuntimeException {
    private final int timeoutMillis;

    public PreviewTimeoutException(int timeoutMillis) {
        super("Preview timeout after " + timeoutMillis + "ms");
        this.timeoutMillis = timeoutMillis;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }
}

