package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record MarketAnalysisMarketStatsRow(
        String symbol,
        String timeframe,
        String event,
        @JsonProperty("condition_name") String conditionName,
        @JsonProperty("condition_value") String conditionValue,
        String target,
        String split,
        Integer n,
        Integer successes,
        @JsonProperty("p_hat") Double pHat,
        @JsonProperty("ci_low") Double ciLow,
        @JsonProperty("ci_high") Double ciHigh,
        Double lift,
        String start,
        String end,
        @JsonProperty("spec_id") String specId,
        @JsonProperty("dataset_id") String datasetId,
        @JsonProperty("created_at") Instant createdAt
) {
}
