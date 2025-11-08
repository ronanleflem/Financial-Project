package finance.project.api.dataimport.ingestion;

import finance.project.api.enums.MarketType;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.CandleAggregationService;
import finance.project.api.services.CandleService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabentoCsvImportService {

    private static final List<String> TARGET_TIMEFRAMES = List.of(
            "3min", "5min", "10min", "15min", "30min",
            "1h", "2h", "4h", "8h", "12h", "daily", "weekly", "monthly"
    );

    private static final DateTimeFormatter DATASET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
            .withZone(ZoneOffset.UTC);

    private final CandleService candleService;
    private final CandleAggregationService candleAggregationService;

    public void importCsv(String symbol, String timeframe, Instant start, Instant end, String datasetHint) {
        String dataset = resolveDataset(start, datasetHint);
        LocalDateTime startUtc = LocalDateTime.ofInstant(start, ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofInstant(end, ZoneOffset.UTC);
        log.info("[Databento CSV] Import {} {} using dataset '{}' ({} to {})", symbol, timeframe, dataset, startUtc, endUtc);

        List<CandleDTO> baseCandles = candleService.loadCsvCME(symbol, timeframe, dataset);
        if (baseCandles.isEmpty()) {
            log.warn("[Databento CSV] Dataset '{}' returned no candles for {} {}", dataset, symbol, timeframe);
            return;
        }

        for (String target : TARGET_TIMEFRAMES) {
            if (target.equalsIgnoreCase(timeframe)) {
                continue;
            }
            try {
                List<CandleDTO> aggregated = candleAggregationService.aggregateCandles(baseCandles, target, MarketType.CME);
                if (aggregated.isEmpty()) {
                    log.debug("[Databento CSV] No aggregated candles for timeframe {}", target);
                    continue;
                }
                candleService.saveCandlesToDatabase(aggregated, symbol, target);
            } catch (IllegalArgumentException ex) {
                log.warn("[Databento CSV] Skipping timeframe {}: {}", target, ex.getMessage());
            }
        }
    }

    private String resolveDataset(Instant start, String datasetHint) {
        if (StringUtils.hasText(datasetHint)) {
            return datasetHint;
        }
        if (start == null) {
            return "data_unknown";
        }
        return "data_" + DATASET_FORMATTER.format(start);
    }
}
