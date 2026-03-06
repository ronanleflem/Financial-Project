package finance.project.api.model.marketanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MarketAnalysisRunListResponse(
        List<MarketAnalysisRunItem> items,
        int page,
        int size,
        @JsonProperty("total_elements") long totalElements,
        @JsonProperty("total_pages") int totalPages,
        String sort
) {
}
