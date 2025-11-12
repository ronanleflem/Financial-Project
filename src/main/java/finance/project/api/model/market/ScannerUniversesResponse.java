package finance.project.api.model.market;

import java.util.List;
import java.util.Map;

public record ScannerUniversesResponse(
        Map<String, String> regions,
        Map<String, IndexUniverse> indices,
        Map<String, Double> minimumMarketCaps
) {
    public record IndexUniverse(String code, String description, String currency, List<String> tickers) {
    }
}
