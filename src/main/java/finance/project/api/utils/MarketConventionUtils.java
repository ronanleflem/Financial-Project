package finance.project.api.utils;

import java.util.Map;

public class MarketConventionUtils {

    // Symbol -> pip factor (multiplicateur pour convertir en pips)
    private static final Map<String, Double> pipFactors = Map.ofEntries(
            Map.entry("EURUSD", 10000.0),
            Map.entry("GBPUSD", 10000.0),
            Map.entry("USDJPY", 100.0),
            Map.entry("BTCUSDT", 100.0),
            Map.entry("ETHUSDT", 100.0),
            Map.entry("XAUUSD", 10.0),
            Map.entry("NASDAQ", 1.0),
            Map.entry("SPX500", 1.0),
            Map.entry("DAX40", 1.0)
    );

    // Symbol -> price precision (nombre de décimales)
    private static final Map<String, Integer> pricePrecisions = Map.ofEntries(
            Map.entry("EURUSD", 5),
            Map.entry("GBPUSD", 5),
            Map.entry("USDJPY", 3),
            Map.entry("BTCUSDT", 2),
            Map.entry("ETHUSDT", 2),
            Map.entry("XAUUSD", 2),
            Map.entry("NASDAQ", 1),
            Map.entry("SPX500", 1),
            Map.entry("DAX40", 1)
    );

    // Symbol -> spread moyen en pips
    private static final Map<String, Double> averageSpreads = Map.ofEntries(
            Map.entry("EURUSD", 0.8),
            Map.entry("GBPUSD", 1.2),
            Map.entry("USDJPY", 1.0),
            Map.entry("BTCUSDT", 15.0),
            Map.entry("ETHUSDT", 10.0),
            Map.entry("XAUUSD", 2.5),
            Map.entry("NASDAQ", 1.0),
            Map.entry("SPX500", 0.8),
            Map.entry("DAX40", 1.5)
    );

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
        return pricePrecisions.getOrDefault(symbol.toUpperCase(), 4);
    }

    public static double getAverageSpread(String symbol) {
        return averageSpreads.getOrDefault(symbol.toUpperCase(), 1.0);
    }

    public static boolean isKnownSymbol(String symbol) {
        return pipFactors.containsKey(symbol.toUpperCase());
    }

    /**
     * Calcule les pips nets d'un trade après déduction du spread et commission.
     *
     * @param rawPips Variation brute en pips (positif = gain)
     * @param symbol Le symbole
     * @param commissionPerTrade En pips (0.0 si aucune commission)
     * @return performance nette en pips
     */
    public static double computeNetPips(double rawPips, String symbol, double commissionPerTrade) {
        double spread = getAverageSpread(symbol);
        return rawPips - spread - commissionPerTrade;
    }
}