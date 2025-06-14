package finance.project.api.utils;

public class PipUtils {

    /**
     * Returns the pip factor for a trading symbol. For example, a factor of
     * 10000 means that one pip equals 0.0001.
     *
     * @param symbol the trading symbol (e.g. "EURUSD", "BTCUSDT")
     * @return the pip factor to apply when converting price differences to pips
     */
    public static int getPipFactor(String symbol) {
        if (symbol == null) {
            return 10000; // default to forex style
        }
        return switch (symbol.toUpperCase()) {
            case "EURUSD", "GBPUSD" -> 10000;  // 1 pip = 0.0001
            case "BTCUSDT", "ETHUSDT" -> 100;    // 1 pip = 0.01
            case "NASDAQ" -> 1;                  // 1 pip = 1 point
            default -> 10000;                     // default for major forex pairs
        };
    }
}