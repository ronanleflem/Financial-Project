package finance.project.api.model.fx;

/**
 * Simple FX quote snapshot with bid/ask information.
 */
public record FxQuote(long tsMillis, double bid, double ask) {
}
