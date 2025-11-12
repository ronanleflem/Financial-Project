package finance.project.api.ibkr.model;

import java.time.Instant;

/**
 * Simple immutable OHLCV bar returned by IBKR historical endpoints.
 */
public record IbkrBar(Instant time, double open, double high, double low, double close, long volume) {
}
