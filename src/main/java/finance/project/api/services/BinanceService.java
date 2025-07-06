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
        String url = UriComponentsBuilder.fromHttpUrl(binanceApiUrl + "/klines")
                .queryParam("symbol", symbol)
                .queryParam("interval", interval)
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
                    .timeframe(interval)
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
        String url = UriComponentsBuilder.fromHttpUrl(binanceApiUrl + "/klines")
                .queryParam("symbol", symbol)
                .queryParam("interval", interval)
                .queryParam("startTime", startDate.toInstant(ZoneOffset.UTC).toEpochMilli())
                .queryParam("endTime", endDate.toInstant(ZoneOffset.UTC).toEpochMilli())
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
                    .timeframe(interval)
                    .build());
        }
        return candles;
    }
}