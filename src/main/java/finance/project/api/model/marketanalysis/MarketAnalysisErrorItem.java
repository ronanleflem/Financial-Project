package finance.project.api.model.marketanalysis;

public record MarketAnalysisErrorItem(
        String field,
        String code,
        String message
) {
}
