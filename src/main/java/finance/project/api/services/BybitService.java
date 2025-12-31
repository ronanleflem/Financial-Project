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
public class BybitService {

    private static final String BASE_URL = "https://api.bybit.com";

    private final RestTemplate restTemplate;

    public List<CandleDTO> getHistoricalCandlesInRange(
            String symbol,
            String timeframe,
            LocalDateTime start,
            LocalDateTime end
    ) {
        String interval = mapToBybitInterval(timeframe);
        if (interval == null) {
            log.warn("[Bybit] Unsupported timeframe '{}' -> returning empty list", timeframe);
            return List.of();
        }

        long startMs = start.toInstant(ZoneOffset.UTC).toEpochMilli();
        long endMs = end.toInstant(ZoneOffset.UTC).toEpochMilli();

        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/v5/market/kline")
                .queryParam("category", "spot")
                .queryParam("symbol", symbol.toUpperCase())
                .queryParam("interval", interval)
                .queryParam("start", startMs)
                .queryParam("end", endMs)
                .queryParam("limit", 1000)
                .build(true)
                .toUri();

        log.info("[Bybit] Fetching kline symbol={} interval={} start={} end={}",
                symbol, interval, start, end);

        try {
            RequestEntity<Void> req = new RequestEntity<>(HttpMethod.GET, uri);
            ResponseEntity<BybitResponse> resp = restTemplate.exchange(
                    req,
                    new ParameterizedTypeReference<>() {}
            );

            BybitResponse body = resp.getBody();
            if (body == null || body.result == null || body.result.list == null || body.result.list.isEmpty()) {
                log.info("[Bybit] No candles returned for {} {}", symbol, timeframe);
                return List.of();
            }

            List<CandleDTO> result = new ArrayList<>(body.result.list.size());
            for (List<String> k : body.result.list) {
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
                        .timeframe(interval)
                        .build();
                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            log.warn("[Bybit] Error fetching kline for {} {}: {}", symbol, timeframe, e.getMessage());
            return List.of();
        }
    }

    private String mapToBybitInterval(String timeframe) {
        if (timeframe == null) {
            return null;
        }
        return switch (timeframe.toLowerCase()) {
            case "1m", "1min" -> "1";
            case "3m" -> "3";
            case "5m" -> "5";
            case "15m" -> "15";
            case "30m" -> "30";
            case "1h", "60m" -> "60";
            case "2h", "120m" -> "120";
            case "4h", "240m" -> "240";
            case "6h", "360m" -> "360";
            case "12h", "720m" -> "720";
            case "1d", "daily" -> "D";
            case "1w", "weekly" -> "W";
            case "1mo", "monthly" -> "M";
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

    private static class BybitResponse {
        private String retCode;
        private String retMsg;
        private BybitResult result;

        public String getRetCode() {
            return retCode;
        }

        public void setRetCode(String retCode) {
            this.retCode = retCode;
        }

        public String getRetMsg() {
            return retMsg;
        }

        public void setRetMsg(String retMsg) {
            this.retMsg = retMsg;
        }

        public BybitResult getResult() {
            return result;
        }

        public void setResult(BybitResult result) {
            this.result = result;
        }
    }

    private static class BybitResult {
        private List<List<String>> list;

        public List<List<String>> getList() {
            return list;
        }

        public void setList(List<List<String>> list) {
            this.list = list;
        }
    }
}
