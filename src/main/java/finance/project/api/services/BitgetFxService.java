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
public class BitgetFxService extends AbstractPollingFxService {

    @Value("${bitget.api.url:https://api.bitget.com/api/v2}")
    private String bitgetApiUrl;

    public BitgetFxService() {
        super("bitget");
    }

    @Override
    protected FxQuote fetchQuote(String normalizedSymbol) {
        String url = UriComponentsBuilder.fromHttpUrl(bitgetApiUrl + "/spot/market/ticker")
                .queryParam("symbol", normalizedSymbol)
                .toUriString();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> body = response.getBody();
        if (body == null) {
            return null;
        }
        Object dataObj = body.get("data");
        if (!(dataObj instanceof List<?> dataList) || dataList.isEmpty()) {
            return null;
        }
        Object first = dataList.get(0);
        if (!(first instanceof Map<?, ?> ticker)) {
            return null;
        }
        double bid = parseDouble(ticker.get("bestBid"));
        if (bid == 0.0d) {
            bid = parseDouble(ticker.get("bidPx"));
        }
        double ask = parseDouble(ticker.get("bestAsk"));
        if (ask == 0.0d) {
            ask = parseDouble(ticker.get("askPx"));
        }
        long timestamp = body.containsKey("requestTime")
                ? ((Number) body.get("requestTime")).longValue()
                : Instant.now().toEpochMilli();
        return new FxQuote(timestamp, bid, ask);
    }

    @Override
    protected List<HistBar> fetchBars(String normalizedSymbol, String interval, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, maxBarsPerRequest()));
        String url = UriComponentsBuilder.fromHttpUrl(bitgetApiUrl + "/spot/market/candles")
                .queryParam("symbol", normalizedSymbol)
                .queryParam("granularity", interval)
                .queryParam("limit", safeLimit)
                .toUriString();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            return List.of();
        }
        Object dataObj = body.get("data");
        if (!(dataObj instanceof List<?> rawBars) || rawBars.isEmpty()) {
            return List.of();
        }

        List<HistBar> bars = new ArrayList<>(rawBars.size());
        for (Object entryObj : rawBars) {
            if (!(entryObj instanceof List<?> entry) || entry.size() < 6) {
                continue;
            }
            long openTime = (long) (Double.parseDouble(entry.get(0).toString()) * 1000L);
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
        long seconds = switch (parsed.unit) {
            case SECOND -> parsed.amount;
            case MINUTE -> parsed.amount * 60L;
            case HOUR -> parsed.amount * 3_600L;
            case DAY -> parsed.amount * 86_400L;
            case WEEK -> parsed.amount * 604_800L;
            case MONTH -> parsed.amount * 2_592_000L;
        };
        return Long.toString(seconds);
    }

    @Override
    protected String normalizeSymbol(String pair) {
        String normalized = super.normalizeSymbol(pair);
        if (normalized.endsWith("USD") && !normalized.endsWith("USDT")) {
            normalized = normalized.substring(0, normalized.length() - 3) + "USDT";
        }
        return normalized;
    }

    @Override
    protected int maxBarsPerRequest() {
        return 500;
    }
}
