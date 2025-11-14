package finance.project.api.universe.client;
/*
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
}*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Client minimal pour récupérer les top N cryptos via Coingecko Pro.
 * Utilise RestTemplate + header x-cg-pro-api-key.
 */
@Component
public class CoingeckoUniverseClient {

    private static final Logger log = LoggerFactory.getLogger(CoingeckoUniverseClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;

    public CoingeckoUniverseClient(
            @Value("${external.coingecko.base-url}") String baseUrl,
            @Value("${external.coingecko.api-key}") String apiKey,
            RestTemplate restTemplate
    ) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
    }

    public List<String> fetchTopCryptoSymbols(int limit) {
        int perPage = Math.min(limit, 250);
        String url = baseUrl
                + "/coins/markets"
                + "?vs_currency=usd"
                + "&order=market_cap_desc"
                + "&per_page=" + perPage
                + "&page=1"
                + "&sparkline=false";

        log.info("[Coingecko] Fetching top {} crypto symbols from {}", limit, url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-cg-pro-api-key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<CoinMarket>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<CoinMarket>>() {}
        );

        List<CoinMarket> markets = response.getBody();
        if (markets == null || markets.isEmpty()) {
            log.warn("[Coingecko] Empty response from coins/markets");
            return List.of();
        }

        return markets.stream()
                .map(CoinMarket::symbol)
                .filter(sym -> sym != null && !sym.isBlank())
                .map(sym -> sym.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    public List<CoinCategory> listCategories() {
        String url = baseUrl + "/coins/categories/list";
        log.info("[Coingecko] Fetching coin categories from {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-cg-pro-api-key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<CoinCategory>> resp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<CoinCategory>>() {}
        );

        List<CoinCategory> body = resp.getBody();
        if (body == null || body.isEmpty()) {
            log.warn("[Coingecko] Empty categories list");
            return List.of();
        }
        return body;
    }

    /**
     * DTO minimal pour mapper /coins/markets
     */
    public record CoinMarket(String id, String symbol, String name) {}

    /**
     * DTO minimal pour /coins/categories/list
     */
    public record CoinCategory(String category_id, String name) {}
}


