package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class BinanceService {

    @Value("${binance.api.url:https://api.binance.com/api/v3}")
    private String binanceApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Récupère l'historique des bougies depuis Binance.
     *
     * @param symbol   symbole (ex: BTCUSDT)
     * @param interval intervalle (ex: 1h,4h,1d)
     * @param limit    nombre de bougies à récupérer
     * @return liste de CandleDTO
     */
    public List<CandleDTO> getHistoricalCandles(String symbol, String interval, int limit) {
        String normalizedInterval = normalizeInterval(interval);
        String url = UriComponentsBuilder.fromHttpUrl(binanceApiUrl + "/klines")
                .queryParam("symbol", symbol)
                .queryParam("interval", normalizedInterval)
                .queryParam("limit", limit)
                .toUriString();

        ResponseEntity<List<List<Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        List<List<Object>> body = response.getBody();
        if (body == null) {
            return List.of();
        }

        List<CandleDTO> candles = new ArrayList<>();
        for (List<Object> entry : body) {
            long openTime = ((Number) entry.get(0)).longValue();
            String open = entry.get(1).toString();
            String high = entry.get(2).toString();
            String low = entry.get(3).toString();
            String close = entry.get(4).toString();
            String volume = entry.get(5).toString();

            candles.add(CandleDTO.builder()
                    .date(LocalDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneOffset.UTC))
                    .open(new BigDecimal(open))
                    .high(new BigDecimal(high))
                    .low(new BigDecimal(low))
                    .close(new BigDecimal(close))
                    .volume(new BigDecimal(volume))
                    .symbol(SymbolDTO.builder().symbol(symbol).build())
                    .timeframe(normalizedInterval)
                    .build());
        }
        return candles;
    }

    /**
     * Retrieve historical candles from Binance for a specific time range.
     *
     * @param symbol    trading pair (e.g. BTCUSDT)
     * @param interval  candle interval (e.g. 1h,4h,1d)
     * @param startDate start of the range in UTC
     * @param endDate   end of the range in UTC
     * @return list of CandleDTO
     */
    public List<CandleDTO> getHistoricalCandlesInRange(String symbol, String interval,
                                                       LocalDateTime startDate, LocalDateTime endDate) {
        List<CandleDTO> allCandles = new ArrayList<>();

        String normalizedInterval = normalizeInterval(interval);
        java.time.Duration tfDuration = finance.project.api.utils.DurationUtils.parseTimeframe(normalizedInterval);

        LocalDateTime currentStart = startDate;

        while (!currentStart.isAfter(endDate)) {
            LocalDateTime currentEnd = currentStart.plus(tfDuration.multipliedBy(1000L)).minusNanos(1);
            if (currentEnd.isAfter(endDate)) {
                currentEnd = endDate;
            }

            String url = UriComponentsBuilder.fromHttpUrl(binanceApiUrl + "/klines")
                    .queryParam("symbol", symbol)
                    .queryParam("interval", normalizedInterval)
                    .queryParam("startTime", currentStart.toInstant(ZoneOffset.UTC).toEpochMilli())
                    .queryParam("endTime", currentEnd.toInstant(ZoneOffset.UTC).toEpochMilli())
                    .queryParam("limit", 1000)
                    .toUriString();

            ResponseEntity<List<List<Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            List<List<Object>> body = response.getBody();
            if (body == null || body.isEmpty()) {
                break;
            }

            for (List<Object> entry : body) {
                long openTime = ((Number) entry.get(0)).longValue();
                String open = entry.get(1).toString();
                String high = entry.get(2).toString();
                String low = entry.get(3).toString();
                String close = entry.get(4).toString();
                String volume = entry.get(5).toString();

                allCandles.add(CandleDTO.builder()
                        .date(LocalDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneOffset.UTC))
                        .open(new BigDecimal(open))
                        .high(new BigDecimal(high))
                        .low(new BigDecimal(low))
                        .close(new BigDecimal(close))
                        .volume(new BigDecimal(volume))
                        .symbol(SymbolDTO.builder().symbol(symbol).build())
                        .timeframe(normalizedInterval)
                        .build());
            }

            long lastOpenTime = ((Number) body.get(body.size() - 1).get(0)).longValue();
            currentStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastOpenTime), ZoneOffset.UTC)
                    .plus(tfDuration);

            if (currentStart.isAfter(endDate)) {
                break;
            }
        }

        return allCandles;
    }

    private String normalizeInterval(String interval) {
        if (interval == null) {
            return null;
        }
        return interval.trim().toLowerCase();
    }
}
