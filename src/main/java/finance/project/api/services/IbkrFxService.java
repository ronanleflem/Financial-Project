package finance.project.api.services;


import com.ib.client.*;
import finance.project.api.adapter.IbkrWrapperAdapter;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.ib.client.EWrapper;
import com.ib.client.TickType;
import com.ib.client.TickAttrib;
import com.ib.client.TickAttribLast;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.CommissionAndFeesReport;

@Service
public class IbkrFxService extends IbkrWrapperAdapter {

    private CountDownLatch connectLatch;

    private EReader reader;
    private ExecutorService readerExec;
    private final Object readerLock = new Object();

    private final EClientSocket client;
    private final EJavaSignal signal;
    private final AtomicInteger reqId = new AtomicInteger(1);

    // Buffer live quotes (les derniers N ticks)
    private final int LIVE_BUFFER = 2000;
    private final Deque<FxQuote> liveQuotes = new ArrayDeque<>(LIVE_BUFFER);

    // ======== buffers live bars (reqId -> deque triée) ========
    private final int LIVE_BARS_BUFFER = 3000;
    private final ConcurrentHashMap<Integer, NavigableMap<Long, HistBar>> liveBars = new ConcurrentHashMap<>();

    // Promesses pour les historiques (reqId -> future liste de barres)
    private final ConcurrentHashMap<Integer, CompletableFuture<List<HistBar>>> histFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, List<HistBar>> histBuffers = new ConcurrentHashMap<>();

    private final Object portfolioLock = new Object();
    private volatile PortfolioRequest currentPortfolioRequest;

    private static final java.util.regex.Pattern MULTISPACE = java.util.regex.Pattern.compile("\\s+");
    private static final java.time.format.DateTimeFormatter F_NO_TZ =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private static final java.time.format.DateTimeFormatter F_WITH_TZ =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss VV");

    // ==== Validation barSize (IB impose des valeurs précises)
    private static final Set<String> ALLOWED_BAR_SIZES = Set.of(
            "1 sec","5 secs","10 secs","15 secs","30 secs",
            "1 min","2 mins","3 mins","5 mins","10 mins","15 mins","20 mins","30 mins",
            "1 hour","2 hours","3 hours","4 hours","8 hours",
            "1 day","1W","1M"
    );

    public IbkrFxService() {
        this.signal = new EJavaSignal();
        this.client = new EClientSocket(this, signal);
    }


    /** Connexion et attente du handshake (bloque jusqu’à timeout) */
    public synchronized boolean connectAndWait(String host, int port, int clientId, long timeoutMs) {
        if (client.isConnected()) return true;

        // (ré)initialise le latch pour CE handshake
        this.connectLatch = new CountDownLatch(1);

        client.eConnect(host, port, clientId);
        startReaderIfNeeded();

        try {
            boolean ok = connectLatch.await(timeoutMs, TimeUnit.MILLISECONDS) && client.isConnected();
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override public void connectAck() {
        CountDownLatch l = connectLatch;
        if (l != null) l.countDown();
    }

    @Override public void nextValidId(int orderId) {
        CountDownLatch l = connectLatch;
        if (l != null) l.countDown();
    }

    // ---- démarre le EReader si besoin
    private void startReaderIfNeeded() {
        synchronized (readerLock) {
            if (reader != null) return;
            reader = new EReader(client, signal);
            reader.start();
            readerExec = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ibkr-reader");
                t.setDaemon(true);
                return t;
            });
            readerExec.submit(() -> {
                while (client.isConnected()) {
                    try {
                        signal.waitForSignal();
                        reader.processMsgs();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }
    }
    /** Connexion async "fire-and-forget" (ne bloque pas) */
    public synchronized void connect(String host, int port, int clientId) {
        if (client.isConnected()) return;
        client.eConnect(host, port, clientId);
        startReaderIfNeeded();
    }

    public synchronized void disconnect() {
        try {
            if (client.isConnected()) client.eDisconnect();
        } finally {
            if (readerExec != null) {
                readerExec.shutdownNow();
                readerExec = null;
            }
            reader = null;
        }
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    /** ----------- LIVE FX EURUSD ----------- */

    /** Démarre un flux de bougies 1 minute (EURUSD MIDPOINT) en "keepUpToDate". */
    public int startLiveMinuteBarsEurUsd() {
        int id = reqId.getAndIncrement();
        liveBars.put(id, new ConcurrentSkipListMap<>());
        client.reqHistoricalData(
                id, eurUsdCash(), "",
                "1 D",           // backfill raisonnable
                "1 min",
                "MIDPOINT",      // FX: MIDPOINT
                0,               // useRTH=0 (RTH n'a pas de sens pour FX)
                1,               // formatDate
                true,            // keepUpToDate
                null
        );
        return id;
    }

    private void validateBarSize(String barSize) {
        if (!ALLOWED_BAR_SIZES.contains(barSize)) {
            throw new IllegalArgumentException("barSize invalide. Ex : " + ALLOWED_BAR_SIZES);
        }
    }

    /** Démarre un flux de bougies paramétrable (keepUpToDate = true) */
    public int startLiveBars(String pair, String duration, String barSize, String whatToShow,
                             int useRth, int formatDate /*1=string,2=epoch*/) {
        validateBarSize(barSize);
        int id = reqId.getAndIncrement();
        liveBars.put(id, new ConcurrentSkipListMap<>());

        client.reqHistoricalData(
                id,
                fxCash(pair),
                "",
                duration,       // ex "1 D", "1 W"
                barSize,        // ex "1 min", "5 mins", "1 hour"
                whatToShow,     // "MIDPOINT", "BID_ASK", "TRADES" (pas pour FX)
                useRth,         // 0 recommandé pour FX
                formatDate,     // 2 conseillé (epoch), sinon 1 + parseur robuste
                true,           // keepUpToDate
                null
        );
        return id;
    }

    /** Helper: construit un contrat FX spot depuis "EURUSD" ou "GBPJPY" */
    private Contract fxCash(String pair) {
        String p = pair.trim().toUpperCase();
        if (p.length() != 6) throw new IllegalArgumentException("FX pair attendu ex: EURUSD");
        Contract c = new Contract();
        c.symbol(p.substring(0,3));
        c.secType(Types.SecType.CASH.name());
        c.currency(p.substring(3,6));
        c.exchange("IDEALPRO");
        return c;
    }


    /** Stoppe un flux live bars. */
    public void stopLiveBars(int barsReqId) {
        client.cancelHistoricalData(barsReqId);
        liveBars.remove(barsReqId);
    }

    /** (Optionnel) Récupère seulement la dernière bougie (pratique pour un front) */
    public HistBar getLastLiveBar(int reqId) {
        NavigableMap<Long, HistBar> map = liveBars.get(reqId);
        if (map == null || map.isEmpty()) return null;
        synchronized (map) { return map.lastEntry().getValue(); }
    }

    /** Snapshot des dernières bougies pour un flux donné. */
    public List<HistBar> getRecentLiveBars(int reqId) {
        NavigableMap<Long, HistBar> map = liveBars.get(reqId);
        if (map == null) return List.of();
        synchronized (map) {
            return new ArrayList<>(map.values()); // déjà trié chrono
        }
    }

    private void upsertBar(int reqId, Bar bar) {
        NavigableMap<Long, HistBar> map = liveBars.get(reqId);
        if (map == null) return;
        long ts = parseIbDateTime(bar.time());
        long vol = bar.volume().longValue();
        if (vol < 0) vol = 0; // optionnel: normalise -1 -> 0

        HistBar hb = new HistBar(ts, bar.open(), bar.high(), bar.low(), bar.close(), vol);
        synchronized (map) {
            map.put(ts, hb); // upsert sur la clé timestamp
            while (map.size() > LIVE_BARS_BUFFER) {
                map.pollFirstEntry(); // évite l'inflation mémoire
            }
        }
    }

    /** Démarre le flux live EURUSD (L1). */
    public int startLiveEurUsd() {
        int id = reqId.getAndIncrement();
        client.reqMktData(id, eurUsdCash(), "", false, false, null);
        return id;
    }

    @Override public void marketDataType(int reqId, int marketDataType) {
        // 1=LIVE, 2=FROZEN, 3=DELAYED, 4=DELAYED_FROZEN
        System.out.println("MDT reqId=" + reqId + " type=" + marketDataType);
    }

    public synchronized void setMarketDataType(int type) {
        if (type < 1 || type > 4) {
            throw new IllegalArgumentException("marketDataType must be 1,2,3,4");
        }
        if (!client.isConnected()) {
            throw new IllegalStateException("Not connected to TWS/IB Gateway");
        }
        client.reqMarketDataType(type);
    }

    /** Arrête un flux live. */
    public void stopLive(int liveReqId) {
        client.cancelMktData(liveReqId);
    }

    /** Retourne un snapshot des derniers ticks live (thread-safe). */
    public synchronized List<FxQuote> getRecentLiveQuotes() {
        return new ArrayList<>(liveQuotes);
    }

    /** ----------- HISTORIQUE FX EURUSD ----------- */

    /**
     * Récupère de l’historique EURUSD en MIDPOINT.
     * @param duration    ex: "1 D", "1 W", "1 M"
     * @param barSize     ex: "1 min", "5 mins", "1 hour"
     * @return            liste ordonnée de barres (chronologique)
     */
    public List<HistBar> getHistoricalEurUsd(String duration, String barSize) throws Exception {
        int id = reqId.getAndIncrement();
        CompletableFuture<List<HistBar>> fut = new CompletableFuture<>();
        histFutures.put(id, fut);
        histBuffers.put(id, new ArrayList<>());

        // endDateTime "" = maintenant (serveur)
        client.reqHistoricalData(
                id,
                eurUsdCash(),
                "",                 // endDateTime ("" = now)
                duration,           // "1 D"
                barSize,            // "1 min"
                "MIDPOINT",         // IMPORTANT pour FX
                1,                  // RTH only (1) ou 0
                1,                  // format: 1 = DateTime string
                false,              // keepUpToDate
                null
        );

        // attente (avec timeout raisonnable)
        return fut.get(30, TimeUnit.SECONDS);
    }

    /** ----------- CONTRAT FX EURUSD ----------- */

    private Contract eurUsdCash() {
        Contract c = new Contract();
        c.symbol("EUR");
        c.secType(Types.SecType.CASH.name());  // Spot FX
        c.currency("USD");
        c.exchange("IDEALPRO");          // venue FX chez IB
        return c;
    }

    /** ----------- DTOs ----------- */

    public record FxQuote(long tsMillis, double bid, double ask) { }
    public record HistBar(long tsMillis, double open, double high, double low, double close, long volume) { }
    public record PortfolioSnapshot(
            String account,
            String currency,
            double availableFunds,
            double netLiquidation,
            double grossPositionValue,
            double totalPositionsValue,
            List<PositionBreakdown> positions
    ) { }

    public record PositionBreakdown(
            long conid,
            String symbol,
            String localSymbol,
            String securityType,
            String currency,
            double position,
            double marketPrice,
            double marketValue,
            double allocation
    ) { }

    /** ----------- EWrapper (callbacks) ----------- */

    @Override
    public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
        synchronized (this) {

            double lastBid = -1, lastAsk = -1;
            if (!liveQuotes.isEmpty()) {
                FxQuote last = liveQuotes.peekLast();
                lastBid = last.bid();
                lastAsk = last.ask();
            }

            TickType tt = TickType.get(field);

            // 🔧 Gérer live + delayed + frozen
            if (tt == TickType.BID || tt == TickType.DELAYED_BID) {
                lastBid = price;
            } else if (tt == TickType.ASK || tt == TickType.DELAYED_ASK) {
                lastAsk = price;
            } else {
                return; // on ignore le reste (LAST, OPEN, CLOSE, etc.)
            }

            FxQuote q = new FxQuote(Instant.now().toEpochMilli(), lastBid, lastAsk);
            if (liveQuotes.size() >= LIVE_BUFFER) liveQuotes.pollFirst();
            liveQuotes.addLast(q);
        }
    }

    @Override
    public void historicalData(int reqId, Bar bar) {
        System.out.println("Historical Data : bar.time=" + bar.time());

        // backfill (et parfois la 1re version de la barre courante)
        if (liveBars.containsKey(reqId)) {
            upsertBar(reqId, bar);
            return;
        }
        // ... sinon ton flux historique ponctuel (histBuffers) comme avant
        List<HistBar> buf = histBuffers.get(reqId);
        if (buf != null) {
            long ts = parseIbDateTime(bar.time());
            long vol = bar.volume().longValue(); if (vol < 0) vol = 0;
            buf.add(new HistBar(ts, bar.open(), bar.high(), bar.low(), bar.close(), vol));
        }
    }

    @Override
    public void historicalDataUpdate(int reqId, Bar bar) {
        System.out.println("Historical Data Update : bar.time=" + bar.time());
        upsertBar(reqId, bar);
    }

    @Override
    public void historicalDataEnd(int reqId, String start, String end) {
        List<HistBar> buf = histBuffers.remove(reqId);
        CompletableFuture<List<HistBar>> fut = histFutures.remove(reqId);
        if (fut != null) {
            // Tri chrono si nécessaire (IB envoie souvent déjà trié)
            buf.sort(Comparator.comparingLong(HistBar::tsMillis));
            fut.complete(buf);
        }
    }

    /** ----------- ACCOUNT / PORTFOLIO ----------- */

    public PortfolioSnapshot fetchPortfolioSnapshot(String account) {
        return fetchPortfolioSnapshot(account, 5000);
    }

    public PortfolioSnapshot fetchPortfolioSnapshot(String account, long timeoutMs) {
        if (!client.isConnected()) {
            throw new IllegalStateException("Not connected to TWS/IB Gateway");
        }
        long effectiveTimeout = timeoutMs <= 0 ? 5000 : timeoutMs;
        PortfolioRequest request = new PortfolioRequest(account);
        synchronized (portfolioLock) {
            if (currentPortfolioRequest != null) {
                throw new IllegalStateException("Another portfolio request is already in progress");
            }
            currentPortfolioRequest = request;
        }

        try {
            client.reqAccountUpdates(true, request.requestedAccount());
            return request.future().get(effectiveTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            request.markCompleted();
            request.future().completeExceptionally(e);
            throw new RuntimeException("Interrupted while waiting for IBKR portfolio", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            request.markCompleted();
            throw new RuntimeException("Failed to fetch IBKR portfolio: " + cause.getMessage(), cause);
        } catch (TimeoutException e) {
            request.markCompleted();
            request.future().completeExceptionally(e);
            throw new RuntimeException("Timed out waiting for IBKR portfolio", e);
        } finally {
            try {
                client.reqAccountUpdates(false, request.requestedAccount());
            } catch (Exception ignored) {
            }
            synchronized (portfolioLock) {
                currentPortfolioRequest = null;
            }
        }
    }

    @Override
    public void updateAccountValue(String key, String value, String currency, String accountName) {
        PortfolioRequest request = currentPortfolioRequest;
        if (request == null || !request.matchesAccount(accountName)) return;
        if (value == null || value.isBlank()) return;
        try {
            double numericValue = Double.parseDouble(value);
            request.setCurrencyIfAbsent(currency);
            switch (key) {
                case "AvailableFunds" -> request.setAvailableFunds(numericValue);
                case "NetLiquidation" -> request.setNetLiquidation(numericValue);
                case "GrossPositionValue" -> request.setGrossPositionValue(numericValue);
                default -> {
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void updatePortfolio(Contract contract, double position, double marketPrice, double marketValue,
                                double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
        PortfolioRequest request = currentPortfolioRequest;
        if (request == null || !request.matchesAccount(accountName)) return;
        String key = positionKey(contract);
        PortfolioPositionAccumulator accumulator = request.positions().computeIfAbsent(key, k -> new PortfolioPositionAccumulator(contract));
        accumulator.position = position;
        accumulator.marketPrice = marketPrice;
        accumulator.marketValue = marketValue;
        accumulator.averageCost = averageCost;
        accumulator.unrealizedPnl = unrealizedPNL;
        accumulator.realizedPnl = realizedPNL;
    }

    @Override
    public void accountDownloadEnd(String accountName) {
        PortfolioRequest request = currentPortfolioRequest;
        if (request == null || !request.matchesAccount(accountName) || request.future().isDone()) return;

        double totalPositionsValue = request.positions().values().stream()
                .mapToDouble(p -> Math.abs(p.marketValue))
                .sum();

        List<PositionBreakdown> positions = request.positions().values().stream()
                .sorted(Comparator.comparingDouble((PortfolioPositionAccumulator p) -> Math.abs(p.marketValue)).reversed())
                .map(acc -> toPositionBreakdown(acc, totalPositionsValue))
                .toList();

        double grossPositionValue = Double.isNaN(request.grossPositionValue()) ? totalPositionsValue : request.grossPositionValue();

        PortfolioSnapshot snapshot = new PortfolioSnapshot(
                request.effectiveAccount(),
                request.currency(),
                sanitizeDouble(request.availableFunds()),
                sanitizeDouble(request.netLiquidation()),
                sanitizeDouble(grossPositionValue),
                totalPositionsValue,
                positions
        );

        request.markCompleted();
        request.future().complete(snapshot);
    }

    private double sanitizeDouble(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private PositionBreakdown toPositionBreakdown(PortfolioPositionAccumulator acc, double totalPositionsValue) {
        Contract contract = acc.contract;
        double allocation = totalPositionsValue == 0.0 ? 0.0 : Math.abs(acc.marketValue) / totalPositionsValue;
        return new PositionBreakdown(
                contract != null ? contract.conid() : 0,
                contract != null ? nullToEmpty(contract.symbol()) : "",
                contract != null ? nullToEmpty(contract.localSymbol()) : "",
                contract != null ? nullToEmpty(contract.secType()) : "",
                contract != null ? nullToEmpty(contract.currency()) : "",
                acc.position,
                acc.marketPrice,
                acc.marketValue,
                allocation
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String positionKey(Contract contract) {
        if (contract == null) return "UNKNOWN";
        int conid = contract.conid();
        if (conid != 0) {
            return "CONID:" + conid;
        }
        return (nullToEmpty(contract.symbol()) + ':' + nullToEmpty(contract.secType()) + ':' + nullToEmpty(contract.currency()) + ':' + nullToEmpty(contract.exchange())).toUpperCase(Locale.ROOT);
    }

    private static class PortfolioRequest {
        private final String requestedAccount;
        private final CompletableFuture<PortfolioSnapshot> future = new CompletableFuture<>();
        private final Map<String, PortfolioPositionAccumulator> positions = new LinkedHashMap<>();
        private volatile double availableFunds = Double.NaN;
        private volatile double netLiquidation = Double.NaN;
        private volatile double grossPositionValue = Double.NaN;
        private volatile String currency;
        private volatile String effectiveAccount;
        private volatile boolean completed;

        PortfolioRequest(String requestedAccount) {
            this.requestedAccount = requestedAccount == null ? "" : requestedAccount.trim();
        }

        String requestedAccount() {
            return requestedAccount;
        }

        CompletableFuture<PortfolioSnapshot> future() {
            return future;
        }

        Map<String, PortfolioPositionAccumulator> positions() {
            return positions;
        }

        double availableFunds() {
            return availableFunds;
        }

        double netLiquidation() {
            return netLiquidation;
        }

        double grossPositionValue() {
            return grossPositionValue;
        }

        String currency() {
            return currency;
        }

        String effectiveAccount() {
            return effectiveAccount == null ? requestedAccount : effectiveAccount;
        }

        boolean matchesAccount(String accountName) {
            if (completed) return false;
            if (accountName == null || accountName.isBlank()) {
                return requestedAccount.isBlank() || Objects.equals(effectiveAccount, accountName);
            }
            if (!requestedAccount.isBlank() && !requestedAccount.equals(accountName)) {
                return false;
            }
            if (effectiveAccount != null && !effectiveAccount.equals(accountName)) {
                return false;
            }
            if (effectiveAccount == null) {
                effectiveAccount = accountName;
            }
            return true;
        }

        void setCurrencyIfAbsent(String newCurrency) {
            if (this.currency == null && newCurrency != null && !newCurrency.isBlank()) {
                this.currency = newCurrency;
            }
        }

        void setAvailableFunds(double value) {
            this.availableFunds = value;
        }

        void setNetLiquidation(double value) {
            this.netLiquidation = value;
        }

        void setGrossPositionValue(double value) {
            this.grossPositionValue = value;
        }

        void markCompleted() {
            this.completed = true;
        }
    }

    private static class PortfolioPositionAccumulator {
        final Contract contract;
        double position;
        double marketPrice;
        double marketValue;
        double averageCost;
        double unrealizedPnl;
        double realizedPnl;

        PortfolioPositionAccumulator(Contract contract) {
            this.contract = contract;
        }
    }

    /** Conversion IB time → epoch millis (IB renvoie "yyyyMMdd  HH:mm:ss" ou epoch) */
    private long parseIbDateTime(String ibTime) {
        if (ibTime == null || ibTime.isEmpty()) throw new IllegalArgumentException("empty ibTime");

        // 1) epoch seconds ?
        boolean digitsOnly = true;
        for (int i = 0; i < ibTime.length(); i++) {
            char c = ibTime.charAt(i);
            if (c < '0' || c > '9') { digitsOnly = false; break; }
        }
        if (digitsOnly && ibTime.length() <= 10) {
            return Long.parseLong(ibTime) * 1000L;
        }

        // 2) normalise espaces (1 ou 2) et parse
        String s = MULTISPACE.matcher(ibTime.trim()).replaceAll(" ");
        // cas avec fuseau: "yyyyMMdd HH:mm:ss US/Eastern" ou "America/New_York"
        int firstSpace = s.indexOf(' ');
        int secondSpace = s.indexOf(' ', firstSpace + 1);
        if (secondSpace > 0 && secondSpace < s.length() - 1) {
            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(s, F_WITH_TZ);
            return zdt.toInstant().toEpochMilli();
        } else {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, F_NO_TZ);
            return ldt.atZone(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli();
        }
    }

    @Override
    public void commissionAndFeesReport(CommissionAndFeesReport report) {
        System.out.printf("Commission+Fees execId=%s commission=%.6f currency=%s fees=%.6f%n",
                report.execId(),
                report.commissionAndFees(),
                report.currency());
    }
}
