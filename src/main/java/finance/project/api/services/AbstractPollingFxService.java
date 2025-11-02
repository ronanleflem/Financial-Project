package finance.project.api.services;

import finance.project.api.model.fx.FxQuote;
import finance.project.api.model.fx.HistBar;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base implementation for FX market data services that rely on polling HTTP endpoints.
 * It manages connection toggling, request identifiers, live quote/bar buffers and the
 * scheduling of polling tasks. Concrete subclasses only need to supply provider specific
 * HTTP calls and mapping logic.
 */
public abstract class AbstractPollingFxService implements FxMarketDataService {

    private final String provider;
    private final int liveQuoteBufferSize;
    private final int liveBarsBufferSize;

    protected final RestTemplate restTemplate = new RestTemplate();

    private final ScheduledExecutorService scheduler;
    private final Deque<FxQuote> liveQuotes;
    private final ConcurrentHashMap<Integer, NavigableMap<Long, HistBar>> liveBars;
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> liveQuoteTasks;
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> liveBarTasks;
    private final AtomicInteger reqId;

    private volatile boolean connected;

    protected AbstractPollingFxService(String provider) {
        this(provider, 2_000, 3_000, 10_000);
    }

    protected AbstractPollingFxService(String provider, int liveQuoteBufferSize, int liveBarsBufferSize, int startingReqId) {
        this.provider = provider;
        this.liveQuoteBufferSize = liveQuoteBufferSize;
        this.liveBarsBufferSize = liveBarsBufferSize;
        this.scheduler = java.util.concurrent.Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicInteger idx = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, provider + "-fx-" + idx.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
        this.liveQuotes = new ArrayDeque<>(liveQuoteBufferSize);
        this.liveBars = new ConcurrentHashMap<>();
        this.liveQuoteTasks = new ConcurrentHashMap<>();
        this.liveBarTasks = new ConcurrentHashMap<>();
        this.reqId = new AtomicInteger(startingReqId);
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public synchronized void connect(String host, int port, int clientId) {
        this.connected = true;
    }

    @Override
    public synchronized boolean connectAndWait(String host, int port, int clientId, long timeoutMs) {
        connect(host, port, clientId);
        return connected;
    }

    @Override
    public synchronized void disconnect() {
        connected = false;
        liveQuoteTasks.values().forEach(f -> f.cancel(true));
        liveBarTasks.values().forEach(f -> f.cancel(true));
        liveQuoteTasks.clear();
        liveBarTasks.clear();
        liveBars.clear();
        synchronized (liveQuotes) {
            liveQuotes.clear();
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public int startLiveEurUsd() {
        ensureConnected();
        int id = nextReqId();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> fetchAndStoreQuote(defaultPair(), id),
                0,
                quotePollingIntervalSeconds(),
                TimeUnit.SECONDS
        );
        liveQuoteTasks.put(id, future);
        return id;
    }

    @Override
    public void stopLive(int liveReqId) {
        ScheduledFuture<?> future = liveQuoteTasks.remove(liveReqId);
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public List<FxQuote> getRecentLiveQuotes() {
        synchronized (liveQuotes) {
            return new ArrayList<>(liveQuotes);
        }
    }

    @Override
    public int startLiveMinuteBarsEurUsd() {
        return startLiveBars(defaultPair(), "1 D", "1 min", "MIDPOINT", 0, 2);
    }

    @Override
    public int startLiveBars(String pair, String duration, String barSize, String whatToShow, int useRth, int formatDate) {
        ensureConnected();
        Duration tfDuration = resolveBarSizeDuration(barSize);
        if (tfDuration.isZero()) {
            throw new IllegalArgumentException("Bar size duration resolved to zero for " + barSize);
        }
        String interval = mapBarSizeToInterval(barSize);
        String symbol = normalizeSymbol(pair);

        int id = nextReqId();
        NavigableMap<Long, HistBar> buffer = new ConcurrentSkipListMap<>();
        liveBars.put(id, buffer);

        long pollingSeconds = liveBarsPollingIntervalSeconds(tfDuration);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> pollBars(id, symbol, interval), 0, pollingSeconds, TimeUnit.SECONDS);
        liveBarTasks.put(id, future);
        return id;
    }

    @Override
    public List<HistBar> getRecentLiveBars(int reqId) {
        NavigableMap<Long, HistBar> map = liveBars.get(reqId);
        if (map == null) {
            return List.of();
        }
        synchronized (map) {
            return new ArrayList<>(map.values());
        }
    }

    @Override
    public HistBar getLastLiveBar(int reqId) {
        NavigableMap<Long, HistBar> map = liveBars.get(reqId);
        if (map == null || map.isEmpty()) {
            return null;
        }
        synchronized (map) {
            return map.lastEntry().getValue();
        }
    }

    @Override
    public void stopLiveBars(int reqId) {
        ScheduledFuture<?> future = liveBarTasks.remove(reqId);
        if (future != null) {
            future.cancel(true);
        }
        liveBars.remove(reqId);
    }

    @Override
    public List<HistBar> getHistoricalEurUsd(String duration, String barSize) throws Exception {
        ensureConnected();
        Duration range = parseDuration(duration);
        Duration tfDuration = resolveBarSizeDuration(barSize);
        if (tfDuration.isZero()) {
            throw new IllegalArgumentException("Bar size duration resolved to zero for " + barSize);
        }
        long bars = Math.max(1, range.getSeconds() / Math.max(1, tfDuration.getSeconds()));
        bars = Math.min(bars, maxBarsPerRequest());
        String interval = mapBarSizeToInterval(barSize);
        String symbol = normalizeSymbol(defaultPair());
        return fetchBars(symbol, interval, (int) bars);
    }

    protected void ensureConnected() {
        if (!connected) {
            throw new IllegalStateException(provider.toUpperCase(Locale.ROOT) + " service not connected");
        }
    }

    protected int nextReqId() {
        return reqId.getAndIncrement();
    }

    protected long quotePollingIntervalSeconds() {
        return 2L;
    }

    protected long liveBarsPollingIntervalSeconds(Duration barSizeDuration) {
        long seconds = barSizeDuration.getSeconds();
        if (seconds <= 0) {
            return 1L;
        }
        long half = Math.max(1L, seconds / 2);
        return Math.min(seconds, half);
    }

    protected int liveBarsBufferSize() {
        return liveBarsBufferSize;
    }

    protected String defaultPair() {
        return "EURUSD";
    }

    protected String normalizeSymbol(String pair) {
        if (pair == null) {
            throw new IllegalArgumentException("Pair cannot be null");
        }
        return pair.replace("/", "").replace("-", "").trim().toUpperCase(Locale.ROOT);
    }

    protected Duration resolveBarSizeDuration(String barSize) {
        ParsedTemporal parsed = parseTemporal(barSize);
        return switch (parsed.unit) {
            case SECOND -> Duration.ofSeconds(parsed.amount);
            case MINUTE -> Duration.ofMinutes(parsed.amount);
            case HOUR -> Duration.ofHours(parsed.amount);
            case DAY -> Duration.ofDays(parsed.amount);
            case WEEK -> Duration.ofDays(parsed.amount * 7L);
            case MONTH -> Duration.ofDays(parsed.amount * 30L);
        };
    }

    protected Duration parseDuration(String input) {
        ParsedTemporal parsed = parseTemporal(input);
        return switch (parsed.unit) {
            case SECOND -> Duration.ofSeconds(parsed.amount);
            case MINUTE -> Duration.ofMinutes(parsed.amount);
            case HOUR -> Duration.ofHours(parsed.amount);
            case DAY -> Duration.ofDays(parsed.amount);
            case WEEK -> Duration.ofDays(parsed.amount * 7L);
            case MONTH -> Duration.ofDays(parsed.amount * 30L);
        };
    }

    protected ParsedTemporal parseTemporal(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Temporal value cannot be null");
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Temporal value cannot be empty");
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)^(\\d+)\\s*([a-z]+)$").matcher(trimmed);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unsupported temporal format: " + input);
        }
        long amount = Long.parseLong(matcher.group(1));
        String unitToken = matcher.group(2);
        TemporalUnit unit = resolveUnit(unitToken);
        return new ParsedTemporal(amount, unit);
    }

    protected TemporalUnit resolveUnit(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "s", "sec", "secs", "second", "seconds" -> TemporalUnit.SECOND;
            case "m", "min", "mins", "minute", "minutes" -> TemporalUnit.MINUTE;
            case "h", "hour", "hours" -> TemporalUnit.HOUR;
            case "d", "day", "days" -> TemporalUnit.DAY;
            case "w", "week", "weeks" -> TemporalUnit.WEEK;
            case "mo", "mon", "mons", "month", "months" -> TemporalUnit.MONTH;
            default -> {
                if ("M".equals(token)) {
                    yield TemporalUnit.MONTH;
                }
                throw new IllegalArgumentException("Unsupported unit token: " + token);
            }
        };
    }

    protected double parseDouble(Object value) {
        if (value == null) {
            return 0.0d;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    protected int maxBarsPerRequest() {
        return 1_000;
    }

    protected void handlePollingError(Exception e) {
        e.printStackTrace();
    }

    private void fetchAndStoreQuote(String pair, int reqId) {
        try {
            FxQuote quote = fetchQuote(normalizeSymbol(pair));
            if (quote == null) {
                return;
            }
            synchronized (liveQuotes) {
                if (liveQuotes.size() >= liveQuoteBufferSize) {
                    liveQuotes.pollFirst();
                }
                liveQuotes.addLast(quote);
            }
        } catch (Exception e) {
            handlePollingError(e);
        }
    }

    private void pollBars(int id, String symbol, String interval) {
        try {
            List<HistBar> bars = fetchBars(symbol, interval, Math.min(liveBarsBufferSize, maxBarsPerRequest()));
            if (bars == null || bars.isEmpty()) {
                return;
            }
            bars.sort(java.util.Comparator.comparingLong(HistBar::tsMillis));
            NavigableMap<Long, HistBar> map = liveBars.get(id);
            if (map == null) {
                return;
            }
            synchronized (map) {
                for (HistBar bar : bars) {
                    map.put(bar.tsMillis(), bar);
                    while (map.size() > liveBarsBufferSize) {
                        map.pollFirstEntry();
                    }
                }
            }
        } catch (Exception e) {
            handlePollingError(e);
        }
    }

    protected abstract FxQuote fetchQuote(String normalizedSymbol) throws Exception;

    protected abstract List<HistBar> fetchBars(String normalizedSymbol, String interval, int limit) throws Exception;

    protected abstract String mapBarSizeToInterval(String barSize);

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    protected record ParsedTemporal(long amount, TemporalUnit unit) {
    }

    protected enum TemporalUnit {
        SECOND,
        MINUTE,
        HOUR,
        DAY,
        WEEK,
        MONTH
    }
}
