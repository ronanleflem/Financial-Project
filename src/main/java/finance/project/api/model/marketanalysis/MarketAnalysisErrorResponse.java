package finance.project.api.model.marketanalysis;

import java.util.List;

public record MarketAnalysisErrorResponse(
        List<MarketAnalysisErrorItem> errors
) {
}
