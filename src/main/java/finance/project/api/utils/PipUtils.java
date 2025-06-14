package finance.project.api.utils;

public class PipUtils {

    /**
     * Returns the pip factor for a trading symbol. For example, a factor of
     * 10000 means that one pip equals 0.0001.
     *
     * @param symbol the trading symbol (e.g. "EURUSD", "BTCUSDT")
     * @return the pip factor to apply when converting price differences to pips
     */
    public static double getPipFactor(String symbol) {
        return switch (symbol.toUpperCase()) {
            // Forex : variation très petite → 1 pip = 0.0001
            case "EURUSD", "GBPUSD", "USDJPY" -> 10000.0;

            // Or : 1 pip = 0.1
            case "XAUUSD" -> 10.0;

            // Indices/futures/cryptos → on traite le pip comme 1 point
            case "BTCUSDT", "ETHUSDT", "SOLUSDT", "NASDAQ", "US100", "SPX500", "DAX40" -> 1.0;

            // Défaut plus conservateur
            default -> 1.0;
        };
    }

    public static int getPricePrecision(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "EURUSD", "GBPUSD" -> 5;
            case "USDJPY" -> 3;
            case "BTCUSDT", "ETHUSDT" -> 2;
            case "NASDAQ", "SPX500" -> 1;
            default -> 4;
        };
    }
}