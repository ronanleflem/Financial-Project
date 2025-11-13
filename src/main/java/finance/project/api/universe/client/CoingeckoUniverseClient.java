package finance.project.api.universe.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CoingeckoUniverseClient {

    private static final Logger log = LoggerFactory.getLogger(CoingeckoUniverseClient.class);

    public List<String> fetchTopCryptoSymbols(int limit) {
        log.info("[Coingecko] Fetching top {} crypto symbols", limit);
        // TODO: replace with real HTTP call to Coingecko API.
        List<String> symbols = new ArrayList<>();
        List<String> defaults = List.of("BTC", "ETH", "BNB", "XRP", "SOL", "ADA", "DOGE", "MATIC", "DOT", "LTC");
        for (int i = 0; i < Math.min(limit, defaults.size()); i++) {
            symbols.add(defaults.get(i).toUpperCase(Locale.ROOT) + "USDT");
        }
        return symbols;
    }
}
