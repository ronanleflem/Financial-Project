package finance.project.api.model.fx;

/**
 * Immutable OHLCV bar representation for FX style data.
 */
public record HistBar(long tsMillis, double open, double high, double low, double close, long volume) {
}
