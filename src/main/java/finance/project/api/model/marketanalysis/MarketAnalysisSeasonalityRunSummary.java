package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record MarketAnalysisSeasonalityRunSummary(
        @JsonProperty("run_id") String runId,
        @JsonProperty("spec_id") String specId,
        @JsonProperty("dataset_id") String datasetId,
        @JsonProperty("out_dir") String outDir,
        String status,
        @JsonProperty("best_summary") JsonNode bestSummary,
        @JsonProperty("created_at") Instant createdAt
) {
}
