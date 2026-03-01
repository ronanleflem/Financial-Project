package finance.project.api.dataimport.ingestion;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import finance.project.api.entities.Candle;
import finance.project.api.enums.MarketType;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.BybitService;
import finance.project.api.services.CandleAggregationService;
import finance.project.api.services.CandleService;
import finance.project.api.utils.TimeframeUtils;
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
public class BybitHistoricalService {

    private final BybitService bybitService;
    private final CandleService candleService;
    private final CandleAggregationService candleAggregationService;
    private final DeltaLakeExporter deltaLakeExporter;

    public boolean fetchAndSave(DataImportJob job, Instant start, Instant end) {
        String symbol = job.getSymbol();
        String timeframe = job.getTimeframe();
        LocalDateTime startUtc = LocalDateTime.ofInstant(start, ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofInstant(end, ZoneOffset.UTC);
        log.info("[Bybit] Import {} {} from {} to {}", symbol, timeframe, startUtc, endUtc);

        List<CandleDTO> candles;
        try {
            candles = bybitService.getHistoricalCandlesInRange(symbol, timeframe, startUtc, endUtc);
        } catch (Exception e) {
            log.warn("[Bybit] Error fetching candles for {} {}: {}", symbol, timeframe, e.getMessage());
            return false;
        }

        if (candles == null || candles.isEmpty()) {
            log.warn("[Bybit] No candles returned for {} {} between {} and {}", symbol, timeframe, startUtc, endUtc);
            return false;
        }

        candleService.saveCandlesToDatabase(candles, symbol, timeframe);
        deltaLakeExporter.exportCandlesToDelta(job, mapForDelta(candles, timeframe));

        if (!isOneMinuteTimeframe(timeframe)) {
            log.info("[Bybit] Auto-aggregation skipped for source timeframe {} (requires 1min)", timeframe);
            return true;
        }

        try {
            List<CandleDTO> aggregated =
                    candleAggregationService.aggregateCandles(candles, timeframe, MarketType.CRYPTO);
            if (!aggregated.isEmpty()) {
                candleService.saveCandlesToDatabase(aggregated, symbol, timeframe);
            }
        } catch (IllegalArgumentException ex) {
            log.warn("[Bybit] Timeframe {} not supported for aggregation: {}", timeframe, ex.getMessage());
        }

        return true;
    }

    private List<Candle> mapForDelta(List<CandleDTO> candles, String timeframe) {
        List<Candle> entities = new java.util.ArrayList<>(candles.size());
        String normalized = TimeframeUtils.mapToCustomTimeframe(timeframe);
        for (CandleDTO dto : candles) {
            if (dto == null) {
                continue;
            }
            Candle candle = new Candle();
            candle.setTimeframe(normalized);
            candle.setDate(dto.getDate());
            candle.setOpen(dto.getOpen());
            candle.setClose(dto.getClose());
            candle.setHigh(dto.getHigh());
            candle.setLow(dto.getLow());
            candle.setVolume(dto.getVolume());
            entities.add(candle);
        }
        return entities;
    }


    private boolean isOneMinuteTimeframe(String timeframe) {
        if (timeframe == null) {
            return false;
        }
        String normalized = timeframe.trim().toLowerCase();
        return "1m".equals(normalized) || "1min".equals(normalized);
    }
}
