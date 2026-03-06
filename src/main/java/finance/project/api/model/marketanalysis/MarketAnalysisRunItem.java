package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record MarketAnalysisRunItem(
        @JsonProperty("run_id") String runId,
        @JsonProperty("request_id") String requestId,
        @JsonProperty("spec_type") String specType,
        String status,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("finished_at") Instant finishedAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("error_message") String errorMessage,
        Integer attempts,
        @JsonProperty("max_attempts") Integer maxAttempts,
        @JsonProperty("cancel_requested") Boolean cancelRequested,
        @JsonProperty("persistence_enabled") Boolean persistenceEnabled,
        @JsonProperty("spec_id") String specId,
        @JsonProperty("dataset_id") String datasetId
) {
}
