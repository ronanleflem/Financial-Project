package finance.project.api.dataimport.ingestion;

import com.ib.client.Contract;
import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.ibkr.IbkrRequestException;
import finance.project.api.ibkr.model.IbkrBar;
import finance.project.api.ibkr.model.ResolvedInstrument;
import finance.project.api.model.market.OhlcBar;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.IbkrFxService;
import finance.project.api.utils.DeltaPathBuilder;
import finance.project.api.utils.TimeframeUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IbkrImportService {

    private static final Logger log = LoggerFactory.getLogger(IbkrImportService.class);
    private static final DateTimeFormatter IB_END_DATETIME =
            DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final IbkrFxService ibkrService;
    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;
    private final DeltaLakeExporter deltaLakeExporter;
    private final DeltaPathBuilder deltaPathBuilder;

    public IbkrImportService(IbkrFxService ibkrService,
                             CandleRepository candleRepository,
                             SymbolRepository symbolRepository,
                             DeltaLakeExporter deltaLakeExporter,
                             DeltaPathBuilder deltaPathBuilder) {
        this.ibkrService = ibkrService;
        this.candleRepository = candleRepository;
        this.symbolRepository = symbolRepository;
        this.deltaLakeExporter = deltaLakeExporter;
        this.deltaPathBuilder = deltaPathBuilder;
    }

    @Transactional
    public void fetchAndSave(DataImportJob job, Instant start, Instant end) {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("runId", runId);

        try {
            String symbolCode = job.getSymbol();
            boolean isEtf = job.getAssetClass() != null && job.getAssetClass().equalsIgnoreCase("ETF");
            log.info("[IBKR] Fetching candles for symbol={} timeframe={} range={} -> {}", symbolCode, job.getTimeframe(), start, end);

            String baseSymbol = symbolCode;
            String currencyFromSymbol = job.getCurrency();
            if (symbolCode != null && symbolCode.contains(":")) {
                String[] parts = symbolCode.split(":", 2);
                baseSymbol = parts[0];
                if (currencyFromSymbol == null || currencyFromSymbol.isBlank()) {
                    currencyFromSymbol = parts[1];
                }
            }
            String normalizedBaseSymbol = baseSymbol != null ? baseSymbol.trim() : baseSymbol;
            if (isIsin(normalizedBaseSymbol)) {
                normalizedBaseSymbol = normalizedBaseSymbol.toUpperCase(Locale.ROOT);
            }

            Optional<Symbol> symbolOpt = Optional.empty();
            if (isEtf && isIsin(normalizedBaseSymbol)) {
                symbolOpt = symbolRepository.findByIsin(normalizedBaseSymbol);
            }
            else if (currencyFromSymbol != null && !currencyFromSymbol.isBlank()) {
                symbolOpt = symbolRepository.findBySymbolAndCurrency(normalizedBaseSymbol, currencyFromSymbol);
            }
            if (symbolOpt.isEmpty()) {
                symbolOpt = symbolRepository.findBySymbol(normalizedBaseSymbol);
            }
            if (symbolOpt.isEmpty()) {
                log.warn("[IBKR] Symbol {} not found in repository. Skipping import.", symbolCode);
                return;
            }

            FetchResult fetchResult = fetchIbkrHistory(symbolCode, job.getAssetClass(), start, end, job.getTimeframe(), symbolOpt.orElse(null),runId);
            List<OhlcBar> bars = fetchResult.bars();
            if (bars.isEmpty()) {
                log.info("[IBKR] No bars returned for symbol={} timeframe={}", symbolCode, job.getTimeframe());
                return;
            }

            Symbol symbol = symbolOpt.get();
            ResolvedInstrument resolved = fetchResult.resolvedInstrument();
            updateSymbolFromResolved(symbol, resolved, isEtf, normalizedBaseSymbol);
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

            LocalDateTime min = candles.stream().map(Candle::getDate).min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime max = candles.stream().map(Candle::getDate).max(LocalDateTime::compareTo).orElse(null);
            log.info("[IBKR][SAVE][{}] candles built={} minDate={} maxDate={}", runId, candles.size(), min, max);

            candleRepository.saveAll(candles);
            log.info("[IBKR][SAVE][{}] saved={} (symbol={}, tf={})", runId, candles.size(), symbolCode, job.getTimeframe());
            log.info("[IBKR] Persisted {} candles for symbol={} timeframe={}", candles.size(), symbolCode, job.getTimeframe());
            Map<String, String> metadata = buildMetadata(resolved);
            String deltaPath = deltaPathBuilder.buildPath(resolved, job.getBroker());
            log.info("[IBKR][DELTA][{}] exporting {} candles to path={} metadata={}",
                    runId, candles.size(), deltaPath, metadata);
            deltaLakeExporter.exportCandlesToDelta(job, mapForDelta(candles, job.getTimeframe()), deltaPath, metadata);
        } finally {
            MDC.remove("runId");
        }
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

    private Map<String, String> buildMetadata(ResolvedInstrument resolved) {
        Map<String, String> metadata = new HashMap<>();
        if (resolved == null) {
            metadata.put("resolveStatus", "fallback");
            return metadata;
        }
        metadata.put("conid", String.valueOf(resolved.conid()));
        metadata.put("symbol", resolved.symbol());
        metadata.put("localSymbol", resolved.localSymbol());
        metadata.put("tradingClass", resolved.tradingClass());
        metadata.put("secType", resolved.secType());
        metadata.put("currency", resolved.currency());
        metadata.put("ibPrimaryExch", resolved.ibPrimaryExch());
        metadata.put("normalizedExchange", resolved.normalizedExchange());
        metadata.put("marketType", resolved.marketTypeNormalized());
        metadata.put("resolveStatus", "resolved");
        return metadata;
    }

    private FetchResult fetchIbkrHistory(String symbol,
                                         String assetClass,
                                         Instant start,
                                         Instant end,
                                         String timeframe,
                                         Symbol symbolEntity, String runId) {

        String resolvedSymbol = symbol;
        String currency = "USD";
        if (symbol.contains(":")) {
            String[] parts = symbol.split(":", 2);
            resolvedSymbol = parts[0];
            currency = parts[1];
        }
        String secType = mapAssetClass(assetClass);
        boolean isFx = "CASH".equalsIgnoreCase(secType) || isFxPair(resolvedSymbol);
        if (isFx) {
            secType = "CASH";
        }
        boolean isEtf = assetClass != null && assetClass.equalsIgnoreCase("ETF");

        ResolvedInstrument resolvedInstrument = null;
        try {
            if (isEtf && isIsin(resolvedSymbol)) {
                resolvedInstrument = ibkrService.resolveEtfByIsin(
                        resolvedSymbol,
                        symbolEntity != null ? symbolEntity.getCurrency() : currency,
                        Duration.ofSeconds(15));
            } else {
                if (isFx) {
                    String[] fx = splitFxPair(resolvedSymbol);
                    resolvedInstrument = ibkrService.resolveContractMetadata(fx[0],
                            "CASH",
                            "IDEALPRO",
                            fx[1],
                            Duration.ofSeconds(15));
                } else {
                    resolvedInstrument = ibkrService.resolveContractMetadata(resolvedSymbol,
                            secType,
                            symbolEntity != null ? symbolEntity.getExchange() : null,
                            symbolEntity != null ? symbolEntity.getCurrency() : currency,
                            Duration.ofSeconds(15));
                }
            }
        } catch (IbkrRequestException ex) {
            log.warn("[IBKR] Unable to resolve contract metadata for {}: {}. Using fallback contract.", symbol, ex.getMessage());
        }

        Contract contract = new Contract();
        if (isFx) {
            String[] fx = splitFxPair(resolvedInstrument != null ? resolvedInstrument.symbol() : resolvedSymbol);
            contract.symbol(fx[0]);
            contract.secType("CASH");
            contract.currency(resolvedInstrument != null ? resolvedInstrument.currency() : fx[1]);
            contract.exchange("IDEALPRO");
        } else {
            contract.symbol(resolvedInstrument != null ? resolvedInstrument.symbol() : resolvedSymbol);
            contract.secType(resolvedInstrument != null ? resolvedInstrument.secType() : secType);
            contract.currency(resolvedInstrument != null ? resolvedInstrument.currency() : currency);
            contract.exchange("SMART");
        }
        if (resolvedInstrument != null) {
            contract.conid(resolvedInstrument.conid());
            contract.primaryExch(resolvedInstrument.ibPrimaryExch());
        }

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

        String whatToShow = isFx ? "MIDPOINT" : "TRADES";
        boolean useRth = !isFx;

        log.info("[IBKR][HIST][{}] symbol={} secType={} tf={} start={} end={} diff={} endDateTime='{}' durationStr='{}' barSize='{}' whatToShow={} useRth={}",
                runId,
                symbol,
                secType,
                timeframe,
                start, end,
                diff,
                endDateTime, durationStr, barSize,
                whatToShow, useRth);
        log.info("[IBKR][HIST][{}] contract conid={} symbol={} localSymbol={} secType={} currency={} exchange={} primaryExch={} tradingClass={}",
                runId,
                contract.conid(),
                contract.symbol(),
                contract.localSymbol(),
                contract.secType(),
                contract.currency(),
                contract.exchange(),
                contract.primaryExch(),
                contract.tradingClass());

        List<IbkrBar> bars = ibkrService.requestHistoricalData(contract, endDateTime, durationStr, barSize, whatToShow, useRth, List.of(), Duration.ofSeconds(12));

        var dates = bars.stream()
                .map(b -> b.time().atZone(ZoneOffset.UTC).toLocalDate())
                .sorted()
                .toList();

        log.info("[IBKR][CHK] returnedDays={} first={} last={} contains_2025_01_09={}",
                dates.size(),
                dates.isEmpty() ? null : dates.getFirst(),
                dates.isEmpty() ? null : dates.getLast(),
                dates.contains(java.time.LocalDate.of(2025,1,9))
        );
        if (bars.isEmpty()) {
            log.warn("[IBKR][HIST][{}] IB returned 0 bars (symbol={}, endDateTime={}, duration={}, barSize={})",
                    runId, symbol, endDateTime, durationStr, barSize);
        } else {
            var first = bars.get(0);
            var last  = bars.get(bars.size() - 1);
            log.info("[IBKR][HIST][{}] IB returned {} bars. first={} last={}",
                    runId, bars.size(), first.time(), last.time());

            // Vérif espacements (daily => 1 jour ouvré)
            int gaps = 0;
            for (int i = 1; i < bars.size(); i++) {
                long deltaSec = Duration.between(bars.get(i-1).time(), bars.get(i).time()).getSeconds();
                if (deltaSec > 60L * 60L * 36L) { // > 36h => trou potentiel (weekend/holiday ou bug)
                    gaps++;
                    log.warn("[IBKR][HIST][{}] Gap detected between {} and {} ({} sec)",
                            runId, bars.get(i-1).time(), bars.get(i).time(), deltaSec);
                }
            }
            if (gaps == 0) {
                log.info("[IBKR][HIST][{}] No internal gaps detected in returned series.", runId);
            }
        }

        List<OhlcBar> result = new ArrayList<>(bars.size());
        int kept = 0, dropped = 0;
        for (IbkrBar bar : bars) {
            boolean out = bar.time().isBefore(start) || bar.time().isAfter(end);
            if (out) { dropped++; continue; }
            kept++;
            result.add(new OhlcBar(bar.time(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume()));
        }
        log.info("[IBKR][HIST][{}] After range filter: kept={} dropped={} (start={} end={})",
                runId, kept, dropped, start, end);
        return new FetchResult(result, resolvedInstrument);
    }

    private record FetchResult(List<OhlcBar> bars, ResolvedInstrument resolvedInstrument) {}

    private boolean isIsin(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.trim().toUpperCase(Locale.ROOT).matches("^[A-Z0-9]{12}$");
    }

    private void updateSymbolFromResolved(Symbol symbol,
                                          ResolvedInstrument resolved,
                                          boolean isEtf,
                                          String requestedSymbol) {
        if (symbol == null || resolved == null) {
            return;
        }
        boolean updated = false;
        if (isEtf && isIsin(requestedSymbol)) {
            String normalizedIsin = requestedSymbol.trim().toUpperCase(Locale.ROOT);
            if (!StringUtils.hasText(symbol.getIsin())) {
                symbol.setIsin(normalizedIsin);
                updated = true;
            }
            String resolvedSymbol = resolved.symbol();
            if (StringUtils.hasText(resolvedSymbol)
                    && (isIsin(symbol.getSymbol()) || symbol.getSymbol().equalsIgnoreCase(normalizedIsin))
                    && !resolvedSymbol.equals(symbol.getSymbol())) {
                symbol.setSymbol(resolvedSymbol);
                updated = true;
            }
        }
        if (!StringUtils.hasText(symbol.getExchange()) && StringUtils.hasText(resolved.normalizedExchange())) {
            symbol.setExchange(resolved.normalizedExchange());
            updated = true;
        }
        if (updated) {
            symbolRepository.save(symbol);
        }
    }

    private String mapAssetClass(String assetClass) {
        if (assetClass == null || assetClass.isBlank()) {
            return "STK";
        }
        String upper = assetClass.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "FX", "FOREX" -> "CASH";
            case "ETF", "EQUITY", "STOCK", "STK" -> "STK";
            case "FUT", "FUTURE", "FUTURES" -> "FUT";
            default -> upper;
        };
    }

    private boolean isFxPair(String symbol) {
        if (symbol == null) {
            return false;
        }
        String normalized = symbol.replace("/", "").replace("-", "").trim().toUpperCase(Locale.ROOT);
        return normalized.length() == 6 && normalized.chars().allMatch(Character::isLetter);
    }

    private String[] splitFxPair(String symbol) {
        String normalized = symbol.replace("/", "").replace("-", "").trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 6) {
            return new String[]{normalized, "USD"};
        }
        return new String[]{normalized.substring(0, 3), normalized.substring(3, 6)};
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
