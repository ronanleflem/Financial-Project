package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "StressSourceRunItem")
public record StressSourceRunItem(
        @JsonProperty("run_id")
        @Schema(name = "run_id", example = "run_20260220_001")
        String runId,
        @JsonProperty("spec_type")
        @Schema(name = "spec_type", example = "backtest")
        String specType,
        @Schema(example = "SUCCEEDED")
        String status,
        @JsonProperty("created_at")
        @Schema(name = "created_at")
        Instant createdAt,
        @JsonProperty("finished_at")
        @Schema(name = "finished_at")
        Instant finishedAt,
        String symbol,
        String timeframe,
        @JsonProperty("asset_class")
        @Schema(name = "asset_class")
        String assetClass,
        String currency,
        @JsonProperty("trades_count_estimate")
        @Schema(name = "trades_count_estimate", example = "184")
        Integer tradesCountEstimate
) {
}
