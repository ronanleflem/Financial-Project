package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarketAnalysisResultMeta(
        @JsonProperty("spec_id") String specId,
        @JsonProperty("dataset_id") String datasetId,
        @JsonProperty("out_dir") String outDir,
        String window,
        String start,
        String end,
        String status
) {
}
