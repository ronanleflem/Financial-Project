package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunResultResponse(
        String requestId,
        RunStatusResponse.Status status,
        JsonNode result,
        List<Artifact> artifacts,
        List<String> warnings,
        Error error,
        String message
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Artifact(
            String type,
            String path,
            JsonNode meta
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Error(
            String code,
            String message,
            JsonNode details
    ) {
    }
}

