package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunStatusResponse(
        String requestId,
        Status status,
        Instant updatedAt,
        String message
) {
    public enum Status {
        PENDING,
        RUNNING,
        FAILED,
        COMPLETED
    }
}

