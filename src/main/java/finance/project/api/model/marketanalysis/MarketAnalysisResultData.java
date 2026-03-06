package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record MarketAnalysisResultData(
        @JsonProperty("market_stats_rows") List<MarketAnalysisMarketStatsRow> marketStatsRows,
        @JsonProperty("seasonality_profiles") List<MarketAnalysisSeasonalityProfileRow> seasonalityProfiles,
        @JsonProperty("seasonality_run_summary") MarketAnalysisSeasonalityRunSummary seasonalityRunSummary,
        @JsonProperty("raw_result_json") JsonNode rawResultJson
) {
}
