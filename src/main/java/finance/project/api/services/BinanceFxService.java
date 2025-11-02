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
import java.util.Map;

@Service
public class BinanceFxService extends AbstractPollingFxService {

    @Value("${binance.api.url:https://api.binance.com/api/v3}")
    private String binanceApiUrl;

    public BinanceFxService() {
        super("binance");
    }

    @Override
    protected FxQuote fetchQuote(String normalizedSymbol) {
        String url = UriComponentsBuilder.fromHttpUrl(binanceApiUrl + "/ticker/bookTicker")
                .queryParam("symbol", normalizedSymbol)
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
        double bid = parseDouble(body.get("bidPrice"));
        double ask = parseDouble(body.get("askPrice"));
        long now = Instant.now().toEpochMilli();
        return new FxQuote(now, bid, ask);
    }

    @Override
    protected List<HistBar> fetchBars(String normalizedSymbol, String interval, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, maxBarsPerRequest()));
        String url = UriComponentsBuilder.fromHttpUrl(binanceApiUrl + "/klines")
                .queryParam("symbol", normalizedSymbol)
                .queryParam("interval", interval)
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
            long openTime = ((Number) entry.get(0)).longValue();
            double open = parseDouble(entry.get(1));
            double high = parseDouble(entry.get(2));
            double low = parseDouble(entry.get(3));
            double close = parseDouble(entry.get(4));
            long volume = new BigDecimal(entry.get(5).toString()).longValue();
            bars.add(new HistBar(openTime, open, high, low, close, volume));
        }
        return bars;
    }

    @Override
    protected String mapBarSizeToInterval(String barSize) {
        ParsedTemporal parsed = parseTemporal(barSize);
        return switch (parsed.unit()) {
            case SECOND -> throw new IllegalArgumentException("Binance does not support second-level klines: " + barSize);
            case MINUTE -> parsed.amount() + "m";
            case HOUR -> parsed.amount() + "h";
            case DAY -> parsed.amount() + "d";
            case WEEK -> parsed.amount() + "w";
            case MONTH -> parsed.amount() + "M";
        };
    }

    @Override
    protected String normalizeSymbol(String pair) {
        String normalized = super.normalizeSymbol(pair);
        if (normalized.endsWith("USD") && !normalized.endsWith("USDT")) {
            normalized = normalized.substring(0, normalized.length() - 3) + "USDT";
        }
        return normalized;
    }
}
