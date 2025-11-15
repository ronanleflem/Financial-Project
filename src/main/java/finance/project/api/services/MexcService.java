package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MexcService {

    private static final String BASE_URL = "https://api.mexc.com";

    private final RestTemplate restTemplate;

    /**
     * Récupère les bougies spot MEXC sur l’intervalle [start, end].
     *
     * Mapping endpoint : GET /api/v3/klines
     * Doc : MEXC Spot V3 - Kline/Candlestick Data
     */
    public List<CandleDTO> getHistoricalCandlesInRange(
            String symbol,
            String timeframe,
            LocalDateTime start,
            LocalDateTime end
    ) {
        String interval = mapToMexcInterval(timeframe);
        if (interval == null) {
            log.warn("[MEXC] Unsupported timeframe '{}' -> returning empty list", timeframe);
            return List.of();
        }

        long startMs = start.toInstant(ZoneOffset.UTC).toEpochMilli();
        long endMs = end.toInstant(ZoneOffset.UTC).toEpochMilli();

        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/api/v3/klines")
                .queryParam("symbol", symbol.toUpperCase())
                .queryParam("interval", interval)
                .queryParam("startTime", startMs)
                .queryParam("endTime", endMs)
                .queryParam("limit", 1000) // max MEXC
                .build(true)
                .toUri();

        log.info("[MEXC] Fetching klines symbol={} interval={} start={} end={}",
                symbol, interval, start, end);

        try {
            RequestEntity<Void> req = new RequestEntity<>(HttpMethod.GET, uri);
            ResponseEntity<List<List<Object>>> resp = restTemplate.exchange(
                    req,
                    new ParameterizedTypeReference<>() {}
            );

            List<List<Object>> body = resp.getBody();
            if (body == null || body.isEmpty()) {
                log.info("[MEXC] No candles returned for {} {}", symbol, timeframe);
                return List.of();
            }

            List<CandleDTO> result = new ArrayList<>(body.size());
            for (List<Object> k : body) {
                // Format MEXC:
                // 0 open time (ms)
                // 1 open
                // 2 high
                // 3 low
                // 4 close
                // 5 volume
                // 6 close time
                // 7 quote asset volume
                long openTimeMs = asLong(k.get(0));
                Instant ts = Instant.ofEpochMilli(openTimeMs);

                double open = asDouble(k.get(1));
                double high = asDouble(k.get(2));
                double low = asDouble(k.get(3));
                double close = asDouble(k.get(4));
                double volume = asDouble(k.get(5));

                //CandleDTO dto = CandleDTO.of(ts, open, high, low, close, volume);
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
            log.warn("[MEXC] Error fetching klines for {} {}: {}", symbol, timeframe, e.getMessage());
            return List.of();
        }
    }

    /**
     * Map tes timeframes internes vers ceux de MEXC.
     * Adapte selon ce que tu utilises déjà pour Binance.
     */
    private String mapToMexcInterval(String timeframe) {
        if (timeframe == null) return null;
        return switch (timeframe.toLowerCase()) {
            case "1m", "1min" -> "1m";
            case "3m" -> "3m";
            case "5m" -> "5m";
            case "15m" -> "15m";
            case "30m" -> "30m";
            case "1h", "60m" -> "1h";
            case "2h" -> "2h";
            case "4h" -> "4h";
            case "6h" -> "6h";
            case "8h" -> "8h";
            case "12h" -> "12h";
            case "1d", "1day", "24h" -> "1d";
            case "3d" -> "3d";
            case "1w", "1week" -> "1w";
            case "1mo", "1month" -> "1M";
            default -> null;
        };
    }

    private long asLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

    private double asDouble(Object o) {
        if (o == null) return 0d;
        if (o instanceof Number n) return n.doubleValue();
        return new BigDecimal(o.toString()).doubleValue();
    }
}
