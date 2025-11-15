package finance.project.api.universe.client;
/*
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FmbUniverseClient {

    private static final Logger log = LoggerFactory.getLogger(FmbUniverseClient.class);

    public List<String> fetchIndexMembers(String universeCode) {
        log.info("[FMB] Fetching members for universe {}", universeCode);
        // TODO: replace with real HTTP call to the FMB/FMP API.
        return switch (universeCode.toUpperCase()) {
            case "NASDAQ100" -> List.of("AAPL", "MSFT", "GOOGL", "AMZN", "NVDA");
            case "CAC40" -> List.of("MC.PA", "OR.PA", "SAN.PA", "BNP.PA");
            case "SP500", "S&P500", "SPX" -> List.of("AAPL", "MSFT", "GOOGL", "AMZN", "META");
            default -> List.of("AAPL", "MSFT");
        };
    }
}*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Client pour récupérer les constituants d'indices via Financial Modeling Prep (FMP/FMB).
 * Utilise RestTemplate + apikey en query string.
 */
@Component
public class FmbUniverseClient {

    private static final Logger log = LoggerFactory.getLogger(FmbUniverseClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;

    public FmbUniverseClient(
            @Value("${external.fmp.base-url}") String baseUrl,
            @Value("${external.fmp.api-key}") String apiKey,
            RestTemplate restTemplate
    ) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
    }

    public List<String> fetchIndexMembers(String universeCode) {
        String uc = universeCode.toUpperCase(Locale.ROOT);
        String path;

        // Mapping univers -> endpoint FMP
        switch (uc) {
            case "^GSPC", "GSPC", "SP500", "S&P500", "SPX" -> path = "/sp500_constituent";
            case "^NDX", "^IXIC", "NASDAQ100", "NASDAQ", "NDX" -> path = "/nasdaq_constituent";
            case "^DJI", "DJI", "DOW", "DOWJONES" -> path = "/dowjones_constituent";
            // FMP ne gère pas CAC40: pour l'instant on garde un fallback statique
            case "CAC40" -> {
                log.warn("[FMB] CAC40 constituents not available via FMP API, using static fallback");
                return List.of("MC.PA", "OR.PA", "SAN.PA", "BNP.PA");
            }
            default -> {
                log.warn("[FMB] Unknown universe code {}, using default fallback list", universeCode);
                return List.of("AAPL", "MSFT");
            }
        }

        String url = baseUrl + path + "?apikey=" + apiKey;
        log.info("[FMB] Fetching members for universe {} via {}", universeCode, url);

        ResponseEntity<List<FmpConstituent>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<FmpConstituent>>() {}
        );

        List<FmpConstituent> body = response.getBody();
        if (body == null || body.isEmpty()) {
            log.warn("[FMB] Empty constituents for universe {}, path={}", universeCode, path);
            return List.of();
        }

        return body.stream()
                .map(FmpConstituent::symbol)
                .filter(sym -> sym != null && !sym.isBlank())
                .map(sym -> sym.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    public List<IndexInfo> listStockIndexes() {
        String url = baseUrl + "/indexes-list?apikey=" + apiKey;
        log.info("[FMB] Fetching stock indexes list from {}", url);

        ResponseEntity<List<IndexInfo>> resp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IndexInfo>>() {}
        );

        List<IndexInfo> body = resp.getBody();
        if (body == null || body.isEmpty()) {
            log.warn("[FMB] Empty indexes list from {}", url);
            return List.of();
        }
        return body;
    }

    /**
     * DTO minimal pour sp500_constituent / nasdaq_constituent
     */
    public record FmpConstituent(String symbol, String name) {}

    /**
     * DTO minimal pour /indexes-list
     */
    public record IndexInfo(String symbol, String name, String exchange, String currency) {}
}

