package finance.project.api.dataimport.ingestion;

import finance.project.api.enums.MarketType;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.BinanceService;
import finance.project.api.services.CandleAggregationService;
import finance.project.api.services.CandleService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceHistoricalService {

    private static final List<String> TARGET_TIMEFRAMES = List.of(
            "1min", "3min", "5min", "10min", "15min", "30min",
            "1h", "2h", "4h", "8h", "12h", "daily", "weekly", "monthly"
    );

    private final BinanceService binanceService;
    private final CandleService candleService;
    private final CandleAggregationService candleAggregationService;

    public void fetchAndSave(String symbol, String timeframe, Instant start, Instant end) {
        LocalDateTime startUtc = LocalDateTime.ofInstant(start, ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofInstant(end, ZoneOffset.UTC);
        log.info("[Binance] Import {} {} from {} to {}", symbol, timeframe, startUtc, endUtc);

        List<CandleDTO> candles = binanceService.getHistoricalCandlesInRange(symbol, timeframe, startUtc, endUtc);
        if (candles.isEmpty()) {
            log.warn("[Binance] No candles returned for {} {} between {} and {}", symbol, timeframe, startUtc, endUtc);
            return;
        }

        candleService.saveCandlesToDatabase(candles, symbol, timeframe);

        String normalizedSource = normalizeTimeframe(timeframe);
        for (String target : TARGET_TIMEFRAMES) {
            if (target.equalsIgnoreCase(normalizedSource)) {
                continue;
            }
            try {
                List<CandleDTO> aggregated = candleAggregationService.aggregateCandles(candles, target, MarketType.CRYPTO);
                if (aggregated.isEmpty()) {
                    log.debug("[Binance] No aggregated candles produced for timeframe {}", target);
                    continue;
                }
                candleService.saveCandlesToDatabase(aggregated, symbol, target);
            } catch (IllegalArgumentException ex) {
                log.warn("[Binance] Timeframe {} not supported for aggregation: {}", target, ex.getMessage());
            }
        }
    }

    private String normalizeTimeframe(String timeframe) {
        if (timeframe == null) {
            return "";
        }
        return switch (timeframe.toLowerCase()) {
            case "1m" -> "1min";
            case "3m" -> "3min";
            case "5m" -> "5min";
            case "10m" -> "10min";
            case "15m" -> "15min";
            case "30m" -> "30min";
            case "1h", "60m" -> "1h";
            case "2h", "120m" -> "2h";
            case "4h", "240m" -> "4h";
            case "6h", "360m" -> "6h";
            case "8h", "480m" -> "8h";
            case "12h", "720m" -> "12h";
            case "1d", "daily" -> "daily";
            case "1w", "weekly" -> "weekly";
            case "1mo", "monthly" -> "monthly";
            default -> timeframe.toLowerCase();
        };
    }
}
