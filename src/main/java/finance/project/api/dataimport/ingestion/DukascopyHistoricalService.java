package finance.project.api.dataimport.ingestion;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;
import finance.project.api.config.DukascopyProperties;
import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import finance.project.api.entities.Candle;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.CandleService;
import finance.project.api.utils.TimeframeUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DukascopyHistoricalService {

    private final DukascopyProperties properties;
    private final CandleService candleService;
    private final DeltaLakeExporter deltaLakeExporter;
    private final IClient client;
    private final Object historyLock = new Object();
    private volatile CountDownLatch connectLatch;

    public DukascopyHistoricalService(DukascopyProperties properties,
                                      CandleService candleService,
                                      DeltaLakeExporter deltaLakeExporter) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.properties = properties;
        this.candleService = candleService;
        this.deltaLakeExporter = deltaLakeExporter;
        this.client = ClientFactory.getDefaultInstance();
        this.client.setSystemListener(new SystemListener());
    }

    public boolean fetchAndSave(DataImportJob job, Instant start, Instant end) {
        if (job == null) {
            return false;
        }
        if (!hasCredentials()) {
            log.warn("[Dukascopy] Missing JForex credentials. Set dukascopy.username/password.");
            return false;
        }
        if (!ensureConnected()) {
            log.warn("[Dukascopy] Unable to connect to JForex.");
            return false;
        }

        String symbol = job.getSymbol();
        String timeframe = job.getTimeframe();
        Instrument instrument = resolveInstrument(symbol);
        if (instrument == null) {
            log.warn("[Dukascopy] Unsupported instrument for symbol={}", symbol);
            return false;
        }
        Period period = toPeriod(timeframe);
        if (period == null) {
            log.warn("[Dukascopy] Unsupported timeframe={} for symbol={}", timeframe, symbol);
            return false;
        }
        OfferSide side = toOfferSide(properties.getOfferSide());

        List<IBar> bars;
        synchronized (historyLock) {
            bars = fetchBars(instrument, period, side, start, end);
        }

        if (bars == null || bars.isEmpty()) {
            log.warn("[Dukascopy] No bars returned for {} {}", symbol, timeframe);
            return false;
        }

        List<CandleDTO> candles = mapBars(bars, symbol, timeframe, start, end);
        if (candles.isEmpty()) {
            log.warn("[Dukascopy] All bars filtered out for {} {}", symbol, timeframe);
            return false;
        }

        if (job.getAssetClass() == null || job.getAssetClass().isBlank()) {
            job.setAssetClass(instrument.getType().name());
        }
        candleService.saveCandlesToDatabase(candles, symbol, timeframe);
        deltaLakeExporter.exportCandlesToDelta(job, mapForDelta(candles, timeframe));
        return true;
    }

    private boolean ensureConnected() {
        if (client.isConnected()) {
            return true;
        }
        CountDownLatch latch = new CountDownLatch(1);
        this.connectLatch = latch;
        try {
            client.connect(properties.getJnlpUrl(), properties.getUsername(), properties.getPassword());
            boolean ok = latch.await(properties.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
            return ok && client.isConnected();
        } catch (Exception e) {
            log.warn("[Dukascopy] Connection failed: {}", e.getMessage());
            return false;
        } finally {
            this.connectLatch = null;
        }
    }

    private List<IBar> fetchBars(Instrument instrument,
                                 Period period,
                                 OfferSide side,
                                 Instant start,
                                 Instant end) {
        CompletableFuture<List<IBar>> future = new CompletableFuture<>();
        IStrategy strategy = new HistoryFetchStrategy(instrument, period, side, start, end, future);
        long strategyId = client.startStrategy(strategy);
        try {
            return future.get(properties.getHistoryTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("[Dukascopy] Historical fetch failed: {}", e.getMessage());
            return List.of();
        } finally {
            if (!future.isDone()) {
                try {
                    client.stopStrategy(strategyId);
                } catch (Exception ex) {
                    log.debug("[Dukascopy] Unable to stop strategy {}: {}", strategyId, ex.getMessage());
                }
            }
        }
    }

    private List<CandleDTO> mapBars(List<IBar> bars,
                                    String symbol,
                                    String timeframe,
                                    Instant start,
                                    Instant end) {
        List<CandleDTO> mapped = new ArrayList<>(bars.size());
        for (IBar bar : bars) {
            if (bar == null) {
                continue;
            }
            Instant barInstant = Instant.ofEpochMilli(bar.getTime());
            if (barInstant.isBefore(start) || barInstant.isAfter(end)) {
                continue;
            }
            LocalDateTime ts = LocalDateTime.ofInstant(barInstant, ZoneOffset.UTC);
            mapped.add(CandleDTO.builder()
                    .date(ts)
                    .open(BigDecimal.valueOf(bar.getOpen()))
                    .high(BigDecimal.valueOf(bar.getHigh()))
                    .low(BigDecimal.valueOf(bar.getLow()))
                    .close(BigDecimal.valueOf(bar.getClose()))
                    .volume(BigDecimal.valueOf(bar.getVolume()))
                    .timeframe(timeframe)
                    .symbolFuture(symbol)
                    .build());
        }
        return mapped;
    }

    private List<Candle> mapForDelta(List<CandleDTO> candles, String timeframe) {
        List<Candle> entities = new ArrayList<>(candles.size());
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

    private Instrument resolveInstrument(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        String normalized = symbol.trim();
        int colon = normalized.indexOf(':');
        if (colon > 0) {
            normalized = normalized.substring(0, colon);
        }
        normalized = normalized.replace("/", "").replace("-", "");
        normalized = normalized.toUpperCase(Locale.ROOT);
        try {
            return Instrument.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Period toPeriod(String timeframe) {
        if (timeframe == null || timeframe.isBlank()) {
            return Period.ONE_HOUR;
        }
        return switch (timeframe.toLowerCase(Locale.ROOT)) {
            case "1s", "1sec", "1second" -> Period.ONE_SEC;
            case "1m", "1min" -> Period.ONE_MIN;
            case "3m", "3min" -> lookupPeriod("THREE_MINS");
            case "5m", "5min" -> Period.FIVE_MINS;
            case "10m", "10min" -> Period.TEN_MINS;
            case "15m", "15min" -> Period.FIFTEEN_MINS;
            case "30m", "30min" -> Period.THIRTY_MINS;
            case "1h" -> Period.ONE_HOUR;
            case "2h" -> lookupPeriod("TWO_HOURS");
            case "4h" -> Period.FOUR_HOURS;
            case "6h" -> lookupPeriod("SIX_HOURS");
            case "8h" -> lookupPeriod("EIGHT_HOURS");
            case "12h" -> lookupPeriod("TWELVE_HOURS");
            case "1d", "1day", "daily" -> Period.DAILY;
            case "1w", "1week", "weekly" -> Period.WEEKLY;
            case "1mo", "1mth", "monthly" -> Period.MONTHLY;
            default -> null;
        };
    }

    private Period lookupPeriod(String fieldName) {
        try {
            return (Period) Period.class.getField(fieldName).get(null);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            return null;
        }
    }

    private OfferSide toOfferSide(String value) {
        if (value == null || value.isBlank()) {
            return OfferSide.BID;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return "ASK".equals(normalized) ? OfferSide.ASK : OfferSide.BID;
    }

    private boolean hasCredentials() {
        return properties.getUsername() != null && !properties.getUsername().isBlank()
                && properties.getPassword() != null && !properties.getPassword().isBlank();
    }

    private class SystemListener implements ISystemListener {
        @Override
        public void onStart(long processId) {
            log.info("[Dukascopy] Strategy {} started", processId);
        }

        @Override
        public void onStop(long processId) {
            log.info("[Dukascopy] Strategy {} stopped", processId);
        }

        @Override
        public void onConnect() {
            CountDownLatch latch = connectLatch;
            if (latch != null) {
                latch.countDown();
            }
            log.info("[Dukascopy] Connected");
        }

        @Override
        public void onDisconnect() {
            log.warn("[Dukascopy] Disconnected");
        }
    }

    private static class HistoryFetchStrategy implements IStrategy {
        private final Instrument instrument;
        private final Period period;
        private final OfferSide side;
        private final Instant start;
        private final Instant end;
        private final CompletableFuture<List<IBar>> future;

        private HistoryFetchStrategy(Instrument instrument,
                                     Period period,
                                     OfferSide side,
                                     Instant start,
                                     Instant end,
                                     CompletableFuture<List<IBar>> future) {
            this.instrument = instrument;
            this.period = period;
            this.side = side;
            this.start = start;
            this.end = end;
            this.future = future;
        }

        @Override
        public void onStart(IContext context) throws JFException {
            try {
                context.setSubscribedInstruments(Set.of(instrument), true);
                var history = context.getHistory();
                long from = history.getBarStart(period, start.toEpochMilli());
                long to = history.getBarStart(period, end.toEpochMilli());
                if (to < from) {
                    long tmp = from;
                    from = to;
                    to = tmp;
                }
                List<IBar> bars = history.getBars(instrument, period, side, from, to);
                future.complete(bars);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                context.stop();
            }
        }

        @Override
        public void onStop() {
        }

        @Override
        public void onTick(Instrument instrument, ITick tick) {
        }

        @Override
        public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        }

        @Override
        public void onMessage(IMessage message) {
        }

        @Override
        public void onAccount(IAccount account) {
        }
    }
}
