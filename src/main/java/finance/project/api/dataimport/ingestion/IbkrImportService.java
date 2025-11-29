package finance.project.api.dataimport.ingestion;

import com.ib.client.Contract;
import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.ibkr.model.IbkrBar;
import finance.project.api.model.market.OhlcBar;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.IbkrFxService;
import finance.project.api.utils.TimeframeUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IbkrImportService {

    private static final Logger log = LoggerFactory.getLogger(IbkrImportService.class);
    private static final DateTimeFormatter IB_END_DATETIME =
            DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final IbkrFxService ibkrService;
    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;
    private final DeltaLakeExporter deltaLakeExporter;

    public IbkrImportService(IbkrFxService ibkrService,
                             CandleRepository candleRepository,
                             SymbolRepository symbolRepository,
                             DeltaLakeExporter deltaLakeExporter) {
        this.ibkrService = ibkrService;
        this.candleRepository = candleRepository;
        this.symbolRepository = symbolRepository;
        this.deltaLakeExporter = deltaLakeExporter;
    }

    @Transactional
    public void fetchAndSave(DataImportJob job, Instant start, Instant end) {
        String symbolCode = job.getSymbol();
        log.info("[IBKR] Fetching candles for symbol={} timeframe={} range={} -> {}", symbolCode, job.getTimeframe(), start, end);

        String baseSymbol = symbolCode;
        String currencyFromSymbol = null;
        if (symbolCode != null && symbolCode.contains(":")) {
            String[] parts = symbolCode.split(":", 2);
            baseSymbol = parts[0];
            currencyFromSymbol = parts[1];
        }

        Optional<Symbol> symbolOpt = Optional.empty();
        if (currencyFromSymbol != null && !currencyFromSymbol.isBlank()) {
            symbolOpt = symbolRepository.findBySymbolAndCurrency(baseSymbol, currencyFromSymbol);
        }
        if (symbolOpt.isEmpty()) {
            symbolOpt = symbolRepository.findBySymbol(baseSymbol);
        }
        if (symbolOpt.isEmpty()) {
            log.warn("[IBKR] Symbol {} not found in repository. Skipping import.", symbolCode);
            return;
        }

        List<OhlcBar> bars = fetchIbkrHistory(symbolCode, job.getAssetClass(), start, end, job.getTimeframe());
        if (bars.isEmpty()) {
            log.info("[IBKR] No bars returned for symbol={} timeframe={}", symbolCode, job.getTimeframe());
            return;
        }

        Symbol symbol = symbolOpt.get();
        List<Candle> candles = new ArrayList<>(bars.size());
        for (OhlcBar bar : bars) {
            LocalDateTime barTime = LocalDateTime.ofInstant(bar.time(), ZoneOffset.UTC);
            candles.add(Candle.builder()
                    .symbol(symbol)
                    .timeframe(job.getTimeframe())
                    .date(barTime)
                    .open(BigDecimal.valueOf(bar.open()))
                    .high(BigDecimal.valueOf(bar.high()))
                    .low(BigDecimal.valueOf(bar.low()))
                    .close(BigDecimal.valueOf(bar.close()))
                    .volume(BigDecimal.valueOf(bar.volume()))
                    .build());
        }

        candleRepository.saveAll(candles);
        log.info("[IBKR] Persisted {} candles for symbol={} timeframe={}", candles.size(), symbolCode, job.getTimeframe());
        deltaLakeExporter.exportCandlesToDelta(job, mapForDelta(candles, job.getTimeframe()));
    }

    private List<Candle> mapForDelta(List<Candle> candles, String timeframe) {
        if (candles == null || candles.isEmpty()) {
            return candles;
        }
        String normalized = TimeframeUtils.mapToCustomTimeframe(timeframe);
        List<Candle> mapped = new ArrayList<>(candles.size());
        for (Candle candle : candles) {
            if (candle == null || candle.getDate() == null) {
                continue;
            }
            Candle deltaCandle = new Candle();
            deltaCandle.setTimeframe(normalized);
            deltaCandle.setDate(candle.getDate());
            deltaCandle.setOpen(candle.getOpen());
            deltaCandle.setClose(candle.getClose());
            deltaCandle.setHigh(candle.getHigh());
            deltaCandle.setLow(candle.getLow());
            deltaCandle.setVolume(candle.getVolume());
            mapped.add(deltaCandle);
        }
        return mapped;
    }

    private List<OhlcBar> fetchIbkrHistory(String symbol,
                                           String assetClass,
                                           Instant start,
                                           Instant end,
                                           String timeframe) {
        String resolvedSymbol = symbol;
        String currency = "USD";
        if (symbol.contains(":")) {
            String[] parts = symbol.split(":", 2);
            resolvedSymbol = parts[0];
            currency = parts[1];
        }
        String secType = mapAssetClass(assetClass);
        Contract contract = new Contract();
        contract.symbol(resolvedSymbol);
        contract.secType(secType);
        contract.currency(currency);
        contract.exchange("SMART");

        String barSize = toIbBarSize(timeframe);
        Duration diff = Duration.between(start, end);
        if (diff.isZero()) {
            diff = Duration.ofHours(1);
        }
        if (diff.isNegative()) {
            diff = diff.negated();
        }
        String durationStr = toIbDuration(diff);
        String endDateTime = IB_END_DATETIME.format(end);

        List<IbkrBar> bars = ibkrService.requestHistoricalData(contract, endDateTime, durationStr, barSize, "TRADES", true, List.of(), Duration.ofSeconds(12));
        List<OhlcBar> result = new ArrayList<>(bars.size());
        for (IbkrBar bar : bars) {
            if (bar.time().isBefore(start) || bar.time().isAfter(end)) {
                continue;
            }
            result.add(new OhlcBar(bar.time(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume()));
        }
        return result;
    }

    private String mapAssetClass(String assetClass) {
        if (assetClass == null || assetClass.isBlank()) {
            return "STK";
        }
        String upper = assetClass.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "ETF", "EQUITY", "STOCK", "STK" -> "STK";
            case "FUT", "FUTURE", "FUTURES" -> "FUT";
            default -> upper;
        };
    }

    private String toIbBarSize(String timeframe) {
        if (timeframe == null || timeframe.isBlank()) {
            return "1 day";
        }
        return switch (timeframe.toLowerCase(Locale.ROOT)) {
            case "1s", "1sec", "1second" -> "1 sec";
            case "5s" -> "5 secs";
            case "10s" -> "10 secs";
            case "15s" -> "15 secs";
            case "30s" -> "30 secs";
            case "1m", "1min" -> "1 min";
            case "2m" -> "2 mins";
            case "3m" -> "3 mins";
            case "5m" -> "5 mins";
            case "10m" -> "10 mins";
            case "15m" -> "15 mins";
            case "30m" -> "30 mins";
            case "1h" -> "1 hour";
            case "2h" -> "2 hours";
            case "4h" -> "4 hours";
            case "1d", "1day" -> "1 day";
            case "1w", "1week" -> "1W";
            case "1mo", "1mth" -> "1M";
            default -> "1 day";
        };
    }

    private String toIbDuration(Duration duration) {
        long seconds = Math.max(60, Math.abs(duration.getSeconds()));
        if (seconds <= 86_400) {
            return seconds + " S";
        }
        long days = Math.max(1, seconds / 86_400);
        if (days <= 30) {
            return days + " D";
        }
        long weeks = Math.max(1, days / 7);
        if (weeks <= 52) {
            return weeks + " W";
        }
        long months = Math.max(1, days / 30);
        if (months <= 24) {
            return months + " M";
        }
        long years = Math.max(1, days / 365);
        return years + " Y";
    }
}
