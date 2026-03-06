package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record MarketAnalysisRunDetailResponse(
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
        @JsonProperty("timeout_seconds") Integer timeoutSeconds,
        @JsonProperty("cancel_requested") Boolean cancelRequested,
        @JsonProperty("canceled_at") Instant canceledAt,
        @JsonProperty("payload_json") JsonNode payloadJson,
        @JsonProperty("progress_json") JsonNode progressJson,
        @JsonProperty("result_json_available") boolean resultJsonAvailable,
        @JsonProperty("persistence_enabled") Boolean persistenceEnabled,
        @JsonProperty("spec_id") String specId,
        @JsonProperty("dataset_id") String datasetId
) {
}
