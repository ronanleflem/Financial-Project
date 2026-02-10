package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import finance.project.api.entities.run.RunLifecycleStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunSubmitResponse(
        String requestId,
        RunLifecycleStatus status
) {
}
