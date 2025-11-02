package finance.project.api.services;

import finance.project.api.model.fx.FxQuote;
import finance.project.api.model.fx.HistBar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CoinbaseFxService extends AbstractPollingFxService {

    private static final Set<Long> ALLOWED_GRANULARITIES = Set.of(60L, 300L, 900L, 3_600L, 21_600L, 86_400L);

    @Value("${coinbase.api.url:https://api.exchange.coinbase.com}")
    private String coinbaseApiUrl;

    public CoinbaseFxService() {
        super("coinbase");
    }

    @Override
    protected FxQuote fetchQuote(String normalizedSymbol) {
        String url = UriComponentsBuilder.fromHttpUrl(coinbaseApiUrl + "/products/" + normalizedSymbol + "/ticker")
                .toUriString();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> body = response.getBody();
        if (body == null || body.isEmpty()) {
            return null;
        }
        double bid = parseDouble(body.get("bid"));
        double ask = parseDouble(body.get("ask"));
        if (bid == 0.0d && body.containsKey("price")) {
            bid = parseDouble(body.get("price"));
        }
        if (ask == 0.0d) {
            ask = bid;
        }
        long timestamp;
        Object time = body.get("time");
        if (time instanceof String timeStr) {
            timestamp = Instant.parse(timeStr).toEpochMilli();
        } else {
            timestamp = Instant.now().toEpochMilli();
        }
        return new FxQuote(timestamp, bid, ask);
    }

    @Override
    protected List<HistBar> fetchBars(String normalizedSymbol, String interval, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, maxBarsPerRequest()));
        String url = UriComponentsBuilder.fromHttpUrl(coinbaseApiUrl + "/products/" + normalizedSymbol + "/candles")
                .queryParam("granularity", interval)
                .queryParam("limit", safeLimit)
                .toUriString();

        ResponseEntity<List<List<Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        List<List<Object>> body = response.getBody();
        if (body == null || body.isEmpty()) {
            return List.of();
        }

        List<HistBar> bars = new ArrayList<>(body.size());
        for (List<Object> entry : body) {
            if (entry.size() < 6) {
                continue;
            }
            long openTime = ((Number) entry.get(0)).longValue() * 1000L;
            double low = parseDouble(entry.get(1));
            double high = parseDouble(entry.get(2));
            double open = parseDouble(entry.get(3));
            double close = parseDouble(entry.get(4));
            long volume = new BigDecimal(entry.get(5).toString()).longValue();
            bars.add(new HistBar(openTime, open, high, low, close, volume));
        }
        return bars;
    }

    @Override
    protected String mapBarSizeToInterval(String barSize) {
        ParsedTemporal parsed = parseTemporal(barSize);
        long seconds = switch (parsed.unit()) {
            case SECOND -> parsed.amount();
            case MINUTE -> parsed.amount() * 60L;
            case HOUR -> parsed.amount() * 3_600L;
            case DAY -> parsed.amount() * 86_400L;
            case WEEK -> parsed.amount() * 604_800L;
            case MONTH -> parsed.amount() * 2_592_000L;
        };
        if (!ALLOWED_GRANULARITIES.contains(seconds)) {
            throw new IllegalArgumentException("Coinbase granularity not supported: " + barSize + " -> " + seconds + " seconds");
        }
        return Long.toString(seconds);
    }

    @Override
    protected String normalizeSymbol(String pair) {
        if (pair == null) {
            throw new IllegalArgumentException("Pair cannot be null");
        }
        String sanitized = pair.trim().toUpperCase(Locale.ROOT).replace("/", "-");
        if (!sanitized.contains("-") && sanitized.length() >= 6) {
            sanitized = sanitized.substring(0, 3) + "-" + sanitized.substring(3);
        }
        return sanitized;
    }

    @Override
    protected int maxBarsPerRequest() {
        return 300;
    }
}
