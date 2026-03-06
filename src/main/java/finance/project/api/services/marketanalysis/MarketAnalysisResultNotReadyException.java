package finance.project.api.services.marketanalysis;

public class MarketAnalysisResultNotReadyException extends RuntimeException {
    public MarketAnalysisResultNotReadyException(String message) {
        super(message);
    }
}
