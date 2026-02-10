package finance.project.api.runs;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

public class PythonDispatchException extends RuntimeException {
    private final Integer httpStatus;
    private final boolean retryable;

    public PythonDispatchException(String message, Integer httpStatus, boolean retryable) {
        super(message);
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public PythonDispatchException(String message, Throwable cause, Integer httpStatus, boolean retryable) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    static PythonDispatchException fromRestClientException(RestClientException ex) {
        if (ex instanceof HttpStatusCodeException http) {
            HttpStatusCode status = http.getStatusCode();
            int code = status.value();
            boolean retryable = (code >= 500) || code == 429;
            return new PythonDispatchException("HTTP " + code + " from python: " + safeBody(http), ex, code, retryable);
        }
        if (ex instanceof ResourceAccessException) {
            return new PythonDispatchException("Connection error to python: " + ex.getMessage(), ex, null, true);
        }
        return new PythonDispatchException("Python dispatch error: " + ex.getMessage(), ex, null, true);
    }

    private static String safeBody(HttpStatusCodeException ex) {
        try {
            String body = ex.getResponseBodyAsString();
            return body == null || body.isBlank() ? "<empty>" : body;
        } catch (Exception ignored) {
            return "<unavailable>";
        }
    }
}

