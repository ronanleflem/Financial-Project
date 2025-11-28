package finance.project.api.universe.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EodhdUniverseClient {

    private static final Logger log = LoggerFactory.getLogger(EodhdUniverseClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public EodhdUniverseClient(RestTemplate restTemplate,
                               @Value("${external.eodhd.base-url}") String baseUrl,
                               @Value("${external.eodhd.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = sanitizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
    }

    public List<EodhdEtfUniverse> listSupportedEtfUniverses() {
        List<EodhdEtfUniverse> universes = new ArrayList<>();
        universes.add(new EodhdEtfUniverse("SP500", "S&P 500 (ETF SPY.US)", "SPY.US", "EQUITY_ETF", "EODHD", 500));
        universes.add(new EodhdEtfUniverse("NASDAQ100", "Nasdaq 100 (ETF QQQ.US)", "QQQ.US", "EQUITY_ETF", "EODHD", 100));
        universes.add(new EodhdEtfUniverse("DOWJONES", "Dow Jones (ETF DIA.US)", "DIA.US", "EQUITY_ETF", "EODHD", 30));
        universes.add(new EodhdEtfUniverse("CAC40", "CAC 40 (ETF CAC.PA)", "CAC.PA", "EQUITY_ETF", "EODHD", 40));
        universes.add(new EodhdEtfUniverse("STOXX600", "Stoxx Europe 600 (ETF EXSA.DE)", "EXSA.DE", "EQUITY_ETF", "EODHD", 600));
        return universes;
    }

    public List<EodhdHolding> fetchEtfHoldings(String etfTicker) {
        String url = String.format("%s/fundamentals/%s?api_token=%s&fmt=json", baseUrl, etfTicker, apiKey);
        log.info("[EODHD] Fetching ETF holdings for {}", etfTicker);

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = resp.getBody();
            if (body == null) {
                log.warn("[EODHD] Empty fundamentals response for {}", etfTicker);
                return List.of();
            }

            Object etfDataObj = body.get("ETF_Data");
            if (!(etfDataObj instanceof Map<?, ?> etfData)) {
                log.warn("[EODHD] No ETF_Data section in fundamentals for {}", etfTicker);
                return List.of();
            }

            Object holdingsObj = etfData.get("Holdings");
            if (!(holdingsObj instanceof List<?> rawHoldings)) {
                log.warn("[EODHD] No Holdings array in ETF_Data for {}", etfTicker);
                return List.of();
            }

            List<EodhdHolding> holdings = new ArrayList<>();
            for (Object o : rawHoldings) {
                if (!(o instanceof Map<?, ?> h)) {
                    continue;
                }
                String code = asString(h.get("Code"));
                String name = asString(h.get("Name"));
                Double weight = asDouble(h.get("Weight"));
                if (code == null || code.isBlank()) {
                    continue;
                }
                holdings.add(new EodhdHolding(code, name, weight));
            }

            return holdings;
        } catch (Exception e) {
            log.warn("[EODHD] Error fetching holdings for {}: {}", etfTicker, e.getMessage());
            return List.of();
        }
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private Double asDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sanitizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://eodhd.com/api";
        }
        String sanitized = baseUrl.trim();
        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        if (sanitized.endsWith("/eod")) {
            sanitized = sanitized.substring(0, sanitized.length() - 4);
        }
        return sanitized;
    }

    public record EodhdEtfUniverse(
            String code,
            String name,
            String etfTicker,
            String type,
            String provider,
            Integer approxSize
    ) {
    }

    public record EodhdHolding(
            String code,
            String name,
            Double weight
    ) {
    }
}
