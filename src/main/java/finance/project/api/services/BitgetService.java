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
public class BitgetService {

    private static final String BASE_URL = "https://api.bitget.com";

    private final RestTemplate restTemplate;

    /**
     * Récupère des candles spot Bitget autour de 'end'.
     *
     * Doc : Spot -> Market -> Get History Candle Data
     * GET /api/spot/v1/market/history-candles
     */
    public List<CandleDTO> getHistoricalCandlesInRange(
            String symbol,
            String timeframe,
            LocalDateTime start,   // pas utilisé directement par l'API mais laissé pour la compat
            LocalDateTime end
    ) {
        String period = mapToBitgetPeriod(timeframe);
        if (period == null) {
            log.warn("[Bitget] Unsupported timeframe '{}' -> returning empty list", timeframe);
            return List.of();
        }

        long endMs = end.toInstant(ZoneOffset.UTC).toEpochMilli();

        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/api/spot/v1/market/history-candles")
                .queryParam("symbol", normalizeBitgetSymbol(symbol))
                .queryParam("period", period)
                .queryParam("endTime", String.valueOf(endMs))
                .queryParam("limit", "200") // max
                .build(true)
                .toUri();

        log.info("[Bitget] Fetching history-candles symbol={} period={} end={}",
                symbol, period, end);

        try {
            RequestEntity<Void> req = new RequestEntity<>(HttpMethod.GET, uri);
            ResponseEntity<BitgetResponse<List<BitgetCandle>>> resp = restTemplate.exchange(
                    req,
                    new ParameterizedTypeReference<>() {}
            );

            BitgetResponse<List<BitgetCandle>> body = resp.getBody();
            if (body == null || body.getData() == null || body.getData().isEmpty()) {
                log.info("[Bitget] No candles returned for {} {}", symbol, timeframe);
                return List.of();
            }

            List<CandleDTO> result = new ArrayList<>(body.getData().size());
            for (BitgetCandle c : body.getData()) {
                Instant ts = Instant.ofEpochMilli(asLong(c.ts));

                double open = asDouble(c.open);
                double high = asDouble(c.high);
                double low = asDouble(c.low);
                double close = asDouble(c.close);
                double volume = asDouble(c.baseVol); // ou quoteVol / usdtVol selon ce que tu préfères

                // ⚠️ Adapte cette ligne à ton vrai CandleDTO
                //CandleDTO dto = CandleDTO.of(ts, open, high, low, close, volume);
                CandleDTO dto = CandleDTO.builder()
                        .date(LocalDateTime.ofInstant(ts, ZoneOffset.UTC))
                        .open(new BigDecimal(open))
                        .high(new BigDecimal(high))
                        .low(new BigDecimal(low))
                        .close(new BigDecimal(close))
                        .volume(new BigDecimal(volume))
                        .symbol(SymbolDTO.builder().symbol(symbol).build())
                        .timeframe(period)
                        .build();
                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            log.warn("[Bitget] Error fetching history-candles for {} {}: {}", symbol, timeframe, e.getMessage());
            return List.of();
        }
    }

    /**
     * Bitget symbol format : souvent 'BTCUSDT_SPBL' pour le spot.
     * Ici on met un mapping simple : si tu stockes 'BTCUSDT', on rajoute '_SPBL'.
     * Tu peux raffiner si besoin.
     */
    private String normalizeBitgetSymbol(String symbol) {
        String s = symbol.toUpperCase();
        if (!s.endsWith("_SPBL")) {
            s = s + "_SPBL";
        }
        return s;
    }

    /**
     * Mapping timeframe interne -> period Bitget.
     * Adapte selon ce que tu utilises côté Java.
     */
    private String mapToBitgetPeriod(String timeframe) {
        if (timeframe == null) return null;
        return switch (timeframe.toLowerCase()) {
            case "1m", "1min" -> "1min";
            case "3m" -> "3min";
            case "5m" -> "5min";
            case "15m" -> "15min";
            case "30m" -> "30min";
            case "1h", "60m" -> "1h";
            case "4h" -> "4h";
            case "12h" -> "12h";
            case "1d", "1day", "24h" -> "1day";
            case "3d" -> "3day";
            case "1w", "1week" -> "1week";
            case "1mo", "1month" -> "1mon";
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

    /**
     * Petites classes internes pour mapper le JSON Bitget :
     *
     * {
     *   "code": "00000",
     *   "msg": "success",
     *   "data": [
     *     {
     *       "open": "2.34",
     *       "high": "2.35",
     *       "low": "2.33",
     *       "close": "2.34",
     *       "quoteVol": "189631.10",
     *       "baseVol": "80862.68",
     *       "usdtVol": "189631.10",
     *       "ts": "1622091360000"
     *     },
     *     ...
     *   ]
     * }
     */
    public static class BitgetResponse<T> {
        private String code;
        private String msg;
        private T data;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }

    public static class BitgetCandle {
        public Object open;
        public Object high;
        public Object low;
        public Object close;
        public Object quoteVol;
        public Object baseVol;
        public Object usdtVol;
        public Object ts;
    }
}
