package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class OkxService {

    private static final String BASE_URL = "https://www.okx.com";

    private final RestTemplate restTemplate;

    public List<CandleDTO> getHistoricalCandlesInRange(
            String symbol,
            String timeframe,
            LocalDateTime start,
            LocalDateTime end
    ) {
        String bar = mapToOkxBar(timeframe);
        if (bar == null) {
            log.warn("[OKX] Unsupported timeframe '{}' -> returning empty list", timeframe);
            return List.of();
        }

        long startMs = start.toInstant(ZoneOffset.UTC).toEpochMilli();
        long endMs = end.toInstant(ZoneOffset.UTC).toEpochMilli();

        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/api/v5/market/history-candles")
                .queryParam("instId", normalizeOkxSymbol(symbol))
                .queryParam("bar", bar)
                .queryParam("after", String.valueOf(startMs))
                .queryParam("before", String.valueOf(endMs))
                .queryParam("limit", "100")
                .build(true)
                .toUri();

        log.info("[OKX] Fetching history-candles symbol={} bar={} start={} end={}",
                symbol, bar, start, end);

        try {
            RequestEntity<Void> req = new RequestEntity<>(HttpMethod.GET, uri);
            ResponseEntity<OkxResponse<List<List<String>>>> resp = restTemplate.exchange(
                    req,
                    new ParameterizedTypeReference<>() {}
            );

            OkxResponse<List<List<String>>> body = resp.getBody();
            if (body == null || body.data == null || body.data.isEmpty()) {
                log.info("[OKX] No candles returned for {} {}", symbol, timeframe);
                return List.of();
            }

            List<CandleDTO> result = new ArrayList<>(body.data.size());
            for (List<String> k : body.data) {
                if (k.size() < 6) {
                    continue;
                }
                long openTimeMs = asLong(k.get(0));
                Instant ts = Instant.ofEpochMilli(openTimeMs);

                double open = asDouble(k.get(1));
                double high = asDouble(k.get(2));
                double low = asDouble(k.get(3));
                double close = asDouble(k.get(4));
                double volume = asDouble(k.get(5));

                CandleDTO dto = CandleDTO.builder()
                        .date(LocalDateTime.ofInstant(ts, ZoneOffset.UTC))
                        .open(new BigDecimal(open))
                        .high(new BigDecimal(high))
                        .low(new BigDecimal(low))
                        .close(new BigDecimal(close))
                        .volume(new BigDecimal(volume))
                        .symbol(SymbolDTO.builder().symbol(symbol).build())
                        .timeframe(bar)
                        .build();
                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            log.warn("[OKX] Error fetching history-candles for {} {}: {}", symbol, timeframe, e.getMessage());
            return List.of();
        }
    }

    private String normalizeOkxSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        String s = symbol.toUpperCase();
        if (s.contains("-")) {
            return s;
        }
        if (s.endsWith("USDT")) {
            return s.substring(0, s.length() - 4) + "-USDT";
        }
        if (s.endsWith("USDC")) {
            return s.substring(0, s.length() - 4) + "-USDC";
        }
        if (s.endsWith("USD")) {
            return s.substring(0, s.length() - 3) + "-USD";
        }
        return s;
    }

    private String mapToOkxBar(String timeframe) {
        if (timeframe == null) {
            return null;
        }
        return switch (timeframe.toLowerCase()) {
            case "1m", "1min" -> "1m";
            case "3m" -> "3m";
            case "5m" -> "5m";
            case "10m" -> "10m";
            case "15m" -> "15m";
            case "30m" -> "30m";
            case "1h", "60m" -> "1H";
            case "2h", "120m" -> "2H";
            case "4h", "240m" -> "4H";
            case "6h", "360m" -> "6H";
            case "12h", "720m" -> "12H";
            case "1d", "daily" -> "1D";
            case "1w", "weekly" -> "1W";
            case "1mo", "monthly" -> "1M";
            default -> null;
        };
    }

    private long asLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(o.toString());
    }

    private double asDouble(Object o) {
        if (o == null) {
            return 0d;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return new BigDecimal(o.toString()).doubleValue();
    }

    private static class OkxResponse<T> {
        private String code;
        private String msg;
        private T data;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
