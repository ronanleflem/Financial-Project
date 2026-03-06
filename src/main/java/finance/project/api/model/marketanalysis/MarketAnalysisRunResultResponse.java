package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarketAnalysisRunResultResponse(
        @JsonProperty("run_id") String runId,
        @JsonProperty("spec_type") String specType,
        String source,
        MarketAnalysisResultMeta meta,
        MarketAnalysisResultData data
) {
}
