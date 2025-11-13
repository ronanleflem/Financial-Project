package finance.project.api.services.market;

import com.ib.client.Contract;
import finance.project.api.ibkr.model.IbkrBar;
import finance.project.api.model.market.OhlcBar;
import finance.project.api.services.BinanceService;
import finance.project.api.services.IbkrFxService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class HistoricalDataService {

    private static final DateTimeFormatter IB_END_DATETIME = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final IbkrFxService ibkrService;
    private final BinanceService binanceService;

    public HistoricalDataService(IbkrFxService ibkrService, BinanceService binanceService) {
        this.ibkrService = ibkrService;
        this.binanceService = binanceService;
    }

    public List<OhlcBar> getHistoricalOhlc(String symbol,
                                           String assetClass,
                                           Instant start,
                                           Instant end,
                                           String timeframe) {
        Objects.requireNonNull(symbol, "symbol");
        Instant effectiveEnd = end != null ? end : Instant.now();
        Instant effectiveStart = start != null ? start : effectiveEnd.minus(Duration.ofDays(30));
        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new IllegalArgumentException("start must be before end");
        }
        if (assetClass != null && assetClass.equalsIgnoreCase("CRYPTO")) {
            return fetchCryptoHistory(symbol, effectiveStart, effectiveEnd, timeframe);
        }
        return fetchIbkrHistory(symbol, assetClass, effectiveStart, effectiveEnd, timeframe);
    }

    private List<OhlcBar> fetchCryptoHistory(String symbol, Instant start, Instant end, String timeframe) {
        // Binance expects milliseconds and a discrete interval string
        var candles = binanceService.getHistoricalCandlesInRange(symbol, timeframe,
                start.atZone(ZoneId.of("UTC")).toLocalDateTime(),
                end.atZone(ZoneId.of("UTC")).toLocalDateTime());
        List<OhlcBar> result = new ArrayList<>(candles.size());
        candles.forEach(candle -> result.add(new OhlcBar(
                candle.getDate().toInstant(ZoneOffset.UTC),
                candle.getOpen().doubleValue(),
                candle.getHigh().doubleValue(),
                candle.getLow().doubleValue(),
                candle.getClose().doubleValue(),
                candle.getVolume().doubleValue()
        )));
        return result;
    }

    private List<OhlcBar> fetchIbkrHistory(String symbol, String assetClass, Instant start, Instant end, String timeframe) {
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
