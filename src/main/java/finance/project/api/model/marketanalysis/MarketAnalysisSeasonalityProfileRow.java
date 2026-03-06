package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record MarketAnalysisSeasonalityProfileRow(
        String symbol,
        String timeframe,
        String dim,
        Integer bin,
        String measure,
        Double score,
        Integer n,
        Double baseline,
        Double lift,
        JsonNode metrics,
        String start,
        String end,
        @JsonProperty("spec_id") String specId,
        @JsonProperty("dataset_id") String datasetId,
        @JsonProperty("created_at") Instant createdAt
) {
}
