package finance.project.api.services;


import com.ib.client.*;
import finance.project.api.adapter.IbkrWrapperAdapter;
import finance.project.api.model.fx.FxQuote;
import finance.project.api.model.fx.HistBar;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.ib.client.TickType;
import com.ib.client.TickAttrib;
import com.ib.client.CommissionAndFeesReport;

@Service
public class IbkrFxService extends IbkrWrapperAdapter implements FxMarketDataService {

    private final AtomicInteger reqId = new AtomicInteger(1);

    private CountDownLatch connectLatch;

    private EReader reader;
    private ExecutorService readerExec;
    private final Object readerLock = new Object();

    // Promesse unique pour une requête en cours (IB n’a pas de reqId ici)
    private final Object positionsLock = new Object();
    private CompletableFuture<List<IbPosition>> positionsFuture;
    private List<IbPosition> positionsBuffer;

    private final EClientSocket client;
    private final EJavaSignal signal;

    // Buffer live quotes (les derniers N ticks)
    private final int LIVE_BUFFER = 2000;
    private final Deque<FxQuote> liveQuotes = new ArrayDeque<>(LIVE_BUFFER);

    // ======== buffers live bars (reqId -> deque triée) ========
    private final int LIVE_BARS_BUFFER = 3000;
    private final ConcurrentHashMap<Integer, NavigableMap<Long, HistBar>> liveBars = new ConcurrentHashMap<>();

    // Promesses pour les historiques (reqId -> future liste de barres)
    private final ConcurrentHashMap<Integer, CompletableFuture<List<HistBar>>> histFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, List<HistBar>> histBuffers = new ConcurrentHashMap<>();

    private volatile List<String> managedAccts = List.of();

    private final Object portfolioLock = new Object();
    private CompletableFuture<List<IbPortfolioLine>> portfolioFuture;
    private final Map<String, IbPortfolioLine> portfolioMap = new HashMap<>();
    public record IbPortfolioLine(
            String account, Contract contract,
            Decimal position, double marketPrice, double marketValue,
            double averageCost, double unrealizedPNL, double realizedPNL
    ) {}

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

    // --- Account Summary snapshot ---
    public record IbAccountSnapshot(
            String account,
            String baseCurrency,
            double availableFunds,
            double excessLiquidity,
            double totalCashValue,
            double netLiquidation
    ) {}

    private final Object accountSummaryLock = new Object();
    private volatile int currentAccountSummaryReqId = -1;
    private final java.util.concurrent.ConcurrentHashMap<String, String> accountSummaryMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicReference<String> accountSummaryAccount = new java.util.concurrent.atomic.AtomicReference<>("");
    private final java.util.concurrent.atomic.AtomicReference<String> accountSummaryBaseFlag = new java.util.concurrent.atomic.AtomicReference<>("BASE");
    private java.util.concurrent.CompletableFuture<Boolean> accountSummaryFuture;

    public IbkrFxService() {
        this.signal = new EJavaSignal();
        this.client = new EClientSocket(this, signal);
    }

    @Override
    public void managedAccounts(String accountsList) {
        // ex: "DU1234567,U123456"
        this.managedAccts = Arrays.stream(accountsList.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        System.out.println("Managed accounts: " + this.managedAccts);
    }
    @Override
    public String getProvider() {
        return "ibkr";
    }


    /** Connexion et attente du handshake (bloque jusqu’à timeout) */
    @Override
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
        if (l != null) {
            l.countDown();
            System.out.println("✅ IBKR connected (connectAck received)");
        }
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
    @Override
    public synchronized void connect(String host, int port, int clientId) {
        if (client.isConnected()) return;
        client.eConnect(host, port, clientId);
        startReaderIfNeeded();
    }

    @Override
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

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    /** ----------- LIVE FX EURUSD ----------- */

    /** Démarre un flux de bougies 1 minute (EURUSD MIDPOINT) en "keepUpToDate". */
    @Override
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
    @Override
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
    @Override
    public void stopLiveBars(int barsReqId) {
        client.cancelHistoricalData(barsReqId);
        liveBars.remove(barsReqId);
    }

    /** (Optionnel) Récupère seulement la dernière bougie (pratique pour un front) */
    @Override
    public HistBar getLastLiveBar(int reqId) {
        NavigableMap<Long, HistBar> map = liveBars.get(reqId);
        if (map == null || map.isEmpty()) return null;
        synchronized (map) { return map.lastEntry().getValue(); }
    }

    /** Snapshot des dernières bougies pour un flux donné. */
    @Override
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
    @Override
    public int startLiveEurUsd() {
        int id = reqId.getAndIncrement();
        client.reqMktData(id, eurUsdCash(), "", false, false, null);
        return id;
    }

    @Override public void marketDataType(int reqId, int marketDataType) {
        // 1=LIVE, 2=FROZEN, 3=DELAYED, 4=DELAYED_FROZEN
        System.out.println("MDT reqId=" + reqId + " type=" + marketDataType);
    }

    @Override
    public boolean supportsMarketDataType() {
        return true;
    }

    @Override
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
    @Override
    public void stopLive(int liveReqId) {
        client.cancelMktData(liveReqId);
    }

    /** Retourne un snapshot des derniers ticks live (thread-safe). */
    @Override
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
    @Override
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

    // ---- DTO interne minimal pour position IB (on le mappera vers TradeView)
    public record IbPosition(
            String account,
            Contract contract,
            double position,
            double avgCost
    ) {}

    // Lance la collecte des positions avec timeout
    public List<IbPosition> fetchOpenPositions(long timeoutMs) throws Exception {
        System.out.println("🟢 Fetching open positions...");
        synchronized (positionsLock) {
            if (positionsFuture != null) {
                throw new IllegalStateException("Positions request already in progress");
            }
            positionsFuture = new CompletableFuture<>();
            positionsBuffer = new ArrayList<>();
            client.reqPositions(); // déclenche callbacks position(...) puis positionEnd()
        }
        try {
            System.out.println("✅ Positions fetch complete, count=" + (positionsFuture.isDone() ? positionsBuffer.size() : 0));
            return positionsFuture.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } finally {
            synchronized (positionsLock) {
                // filet de sécurité
                positionsFuture = null;
                positionsBuffer = null;
            }
        }
    }

    // Optionnel : annuler côté IB si tu veux un bouton "cancel"
    public void cancelPositionsRequest() {
        client.cancelPositions();
    }

// ---- EWrapper callbacks (complète tes @Override déjà présents)

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        synchronized (positionsLock) {
            if (positionsBuffer != null) {
                System.out.printf(
                        "📊 Position received: account=%s, symbol=%s%s, qty=%s, avgCost=%.5f%n",
                        account,
                        contract.symbol(), contract.currency(),
                        pos, avgCost
                );
                positionsBuffer.add(new IbPosition(account, contract, pos, avgCost));
            }
        }
    }

    @Override
    public void positionEnd() {
        System.out.println("📦 PositionEnd reached (positions snapshot complete)");
        synchronized (positionsLock) {
            if (positionsFuture != null) {
                positionsFuture.complete(new ArrayList<>(positionsBuffer));
            }
            positionsFuture = null;
            positionsBuffer = null;
        }
    }

    public List<IbPortfolioLine> fetchPortfolioSnapshot(long timeoutMs) throws Exception {
        System.out.println("🟢 Fetching portfolio snapshot...");
        List<String> accts = this.managedAccts;
        if (accts.isEmpty()) {
            // on peut forcer la requête des comptes si jamais pas encore callback
            client.reqManagedAccts();
            Thread.sleep(200); // petit wait; ou bien attendre un latch dans managedAccounts()
            accts = this.managedAccts;
        }
        synchronized (portfolioLock) {
            if (portfolioFuture != null) throw new IllegalStateException("portfolio request already running");
            portfolioFuture = new CompletableFuture<>();
            portfolioMap.clear();
        }
        // On lance pour CHAQUE compte (IB autorise) :
        for (String acc : accts) client.reqAccountUpdates(true, acc);

        try {
            // on attend un snapshot; IB envoie updatePortfolio + accountDownloadEnd par compte
            // simplif: timeout global; on arrête ensuite
            var deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
                synchronized (portfolioLock) {
                    if (portfolioFuture.isDone()) break;
                }
            }
            synchronized (portfolioLock) {
                if (!portfolioFuture.isDone()) portfolioFuture.complete(new ArrayList<>(portfolioMap.values()));
                System.out.println("✅ Portfolio snapshot complete, count=" + portfolioMap.size());
                return portfolioFuture.get();
            }
        } finally {
            for (String acc : accts) client.reqAccountUpdates(false, acc);
            synchronized (portfolioLock) { portfolioFuture = null; }
        }
    }

    @Override
    public void updatePortfolio(Contract contract,
                                Decimal position,          // <-- Decimal ici
                                double marketPrice,
                                double marketValue,
                                double averageCost,
                                double unrealizedPNL,
                                double realizedPNL,
                                String accountName) {
        synchronized (portfolioLock) {
            if (portfolioFuture == null) return;
            System.out.printf(
                    "💰 Portfolio update: acc=%s, sym=%s%s, pos=%s, px=%.5f, val=%.2f, uPnL=%.2f, rPnL=%.2f%n",
                    accountName,
                    contract.symbol(), contract.currency(),
                    position, marketPrice, marketValue, unrealizedPNL, realizedPNL
            );
            String key = accountName + "|" + contract.conid();
            portfolioMap.put(key, new IbPortfolioLine(
                    accountName, contract, position, marketPrice, marketValue, averageCost, unrealizedPNL, realizedPNL
            ));
        }
    }

    @Override
    public void accountDownloadEnd(String accountName) {
        System.out.println("🏁 Account download end for account: " + accountName);
        synchronized (portfolioLock) {
            if (portfolioFuture != null) {
                // On ne sait pas s’il y a plusieurs comptes; on complète quand on reçoit au moins un end.
                portfolioFuture.complete(new ArrayList<>(portfolioMap.values()));
            }
        }
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        if (reqId != currentAccountSummaryReqId) return;
        accountSummaryAccount.compareAndSet("", account);
        // On stocke par clé "tag|currency" pour gérer multi-devises; on privilégiera BASE plus tard
        accountSummaryMap.put(tag + "|" + currency, value);
        if ("BASE".equals(currency)) accountSummaryBaseFlag.set("BASE");
    }

    @Override
    public void accountSummaryEnd(int reqId) {
        if (reqId != currentAccountSummaryReqId) return;
        var f = accountSummaryFuture;
        if (f != null && !f.isDone()) f.complete(true);
    }

    public IbAccountSnapshot fetchIbAccountSnapshot(long timeoutMs) throws Exception {
        int id = this.reqId.get();
        final String tags = String.join(",",
                "AvailableFunds","ExcessLiquidity","TotalCashValue","NetLiquidation"
        );

        synchronized (accountSummaryLock) {
            if (accountSummaryFuture != null) {
                throw new IllegalStateException("account summary request already running");
            }
            currentAccountSummaryReqId = id;
            accountSummaryMap.clear();
            accountSummaryAccount.set("");
            accountSummaryBaseFlag.set("BASE");
            accountSummaryFuture = new java.util.concurrent.CompletableFuture<>();
        }

        client.reqAccountSummary(id, "All", tags);

        try {
            accountSummaryFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            throw new TimeoutException("Timeout account summary future exception");
        } finally {
            client.cancelAccountSummary(id);
            synchronized (accountSummaryLock) {
                accountSummaryFuture = null;
                currentAccountSummaryReqId = -1;
            }
        }

        // helper pour lire une valeur en priorisant "BASE", sinon la première devise rencontrée
        Function<String, Double> get = (tag) -> {
            String baseKey = tag + "|BASE";
            if (accountSummaryMap.containsKey(baseKey)) {
                try { return Double.parseDouble(accountSummaryMap.get(baseKey)); } catch (Exception ignored) {}
            }
            for (var e : accountSummaryMap.entrySet()) {
                if (e.getKey().startsWith(tag + "|")) {
                    try { return Double.parseDouble(e.getValue()); } catch (Exception ignored) {}
                }
            }
            return 0.0;
        };

        return new IbAccountSnapshot(
                accountSummaryAccount.get(),
                "BASE",
                get.apply("AvailableFunds"),
                get.apply("ExcessLiquidity"),
                get.apply("TotalCashValue"),
                get.apply("NetLiquidation")
        );
    }
}
