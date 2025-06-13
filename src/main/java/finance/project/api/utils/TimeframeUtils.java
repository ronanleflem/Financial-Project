package finance.project.api.utils;

import java.util.HashMap;
import java.util.Map;

public class TimeframeUtils {

    private static final Map<String, String> BINANCE_TO_CUSTOM = new HashMap<>();

    static {
        BINANCE_TO_CUSTOM.put("1m", "1min");
        BINANCE_TO_CUSTOM.put("3m", "3min");
        BINANCE_TO_CUSTOM.put("5m", "5min");
        BINANCE_TO_CUSTOM.put("10m", "10min"); // pas natif Binance
        BINANCE_TO_CUSTOM.put("15m", "15min");
        BINANCE_TO_CUSTOM.put("30m", "30min");
        BINANCE_TO_CUSTOM.put("1h", "1h");
        BINANCE_TO_CUSTOM.put("2h", "2h");
        BINANCE_TO_CUSTOM.put("4h", "4h");
        BINANCE_TO_CUSTOM.put("8h", "8h");
        BINANCE_TO_CUSTOM.put("12h", "12h");
        BINANCE_TO_CUSTOM.put("1d", "daily");
        BINANCE_TO_CUSTOM.put("1w", "weekly");
        BINANCE_TO_CUSTOM.put("1M", "monthly");
    }

    private static final Map<String, String> VALID_CUSTOM_TIMEFRAMES = new HashMap<>();

    static {
        for (String custom : BINANCE_TO_CUSTOM.values()) {
            VALID_CUSTOM_TIMEFRAMES.put(custom, custom);
        }
    }

    public static String mapToCustomTimeframe(String input) {
        // Si c'est déjà un format custom valide, retourne-le directement
        if (VALID_CUSTOM_TIMEFRAMES.containsKey(input)) {
            return input;
        }
        // Sinon, essaie de le convertir depuis Binance
        String mapped = BINANCE_TO_CUSTOM.get(input);
        if (mapped == null) {
            throw new IllegalArgumentException("Timeframe inconnu : " + input);
        }
        return mapped;
    }
}
