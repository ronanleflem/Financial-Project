package finance.project.api.ibkr;

import com.ib.client.Bar;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.ScannerSubscription;
import com.ib.client.TagValue;
import finance.project.api.adapter.IbkrWrapperAdapter;
import finance.project.api.config.IbkrProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Low-level IBKR client dedicated to market data requests (scanners & historical bars).
 * This component centralises the connection, pacing and callback management and exposes
 * synchronous helper methods returning structured results.
 */
@Component
public class IbkrClient extends IbkrWrapperAdapter implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(IbkrClient.class);

    private static final DateTimeFormatter IB_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    private final IbkrProperties properties;
    private final EJavaSignal signal = new EJavaSignal();
    private final EClientSocket client = new EClientSocket(this, signal);
    private final AtomicInteger requestId = new AtomicInteger(10_000);

    private final Map<Integer, CompletableFuture<List<IbkrScannerRow>>> scannerFutures = new ConcurrentHashMap<>();
    private final Map<Integer, List<IbkrScannerRow>> scannerBuffers = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<List<IbkrBar>>> histFutures = new ConcurrentHashMap<>();
    private final Map<Integer, List<IbkrBar>> histBuffers = new ConcurrentHashMap<>();

    private final Object pacingLock = new Object();
    private long lastRequestTime = 0L;

    private ExecutorService readerExecutor;
    private EReader reader;
    private CountDownLatch connectLatch;

    public IbkrClient(IbkrProperties properties) {
        this.properties = properties;
    }

    /** Request identifier used by the next operation. */
    private int nextRequestId() {
        return requestId.updateAndGet(current -> current >= Integer.MAX_VALUE - 1 ? 10_000 : current + 1);
    }

    /** Ensure the socket connection is initialised. */
    public synchronized void ensureConnected() {
        if (client.isConnected()) {
            return;
        }
        this.connectLatch = new CountDownLatch(1);
        log.info("Connecting to IBKR TWS/Gateway at {}:{} with clientId {}", properties.getHost(), properties.getPort(), properties.getClientId());
        client.eConnect(properties.getHost(), properties.getPort(), properties.getClientId());
        startReaderIfNeeded();
        try {
            boolean connected = connectLatch.await(properties.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS) && client.isConnected();
            if (!connected) {
                throw new IbkrClientException("Unable to connect to IBKR within timeout");
            }
            log.info("IBKR connection established");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IbkrClientException("Interrupted while waiting for IBKR connection", e);
        }
    }

    private void startReaderIfNeeded() {
        if (reader != null) {
            return;
        }
        reader = new EReader(client, signal);
        reader.start();
        readerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ibkr-reader");
            t.setDaemon(true);
            return t;
        });
        readerExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    signal.waitForSignal();
                    reader.processMsgs();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    log.error("Error while processing IBKR messages", t);
                }
            }
        });
    }

    private void waitForPacingSlot() {
        long minInterval = properties.getPacingMinIntervalMillis();
        synchronized (pacingLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime;
            if (elapsed < minInterval) {
                try {
                    Thread.sleep(minInterval - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IbkrClientException("Interrupted during pacing wait", e);
                }
            }
            lastRequestTime = System.currentTimeMillis();
        }
    }

    /** Request a market scanner and return the collected rows. */
    public List<IbkrScannerRow> requestScannerData(ScannerSubscription subscription, int limit, Duration timeout, List<TagValue> options) {
        Objects.requireNonNull(subscription, "subscription");
        ensureConnected();
        waitForPacingSlot();

        int reqId = nextRequestId();
        CompletableFuture<List<IbkrScannerRow>> future = new CompletableFuture<>();
        scannerFutures.put(reqId, future);
        scannerBuffers.put(reqId, Collections.synchronizedList(new ArrayList<>()));

        log.debug("Requesting scanner {} with id {}", subscription.scanCode(), reqId);
        client.reqScannerSubscription(reqId, subscription, null, options == null ? List.of() : options);

        try {
            long timeoutMillis = timeout != null ? timeout.toMillis() : properties.getDefaultRequestTimeoutMillis();
            List<IbkrScannerRow> rows = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (limit > 0 && rows.size() > limit) {
                return rows.stream().sorted(Comparator.comparingInt(IbkrScannerRow::rank)).limit(limit).toList();
            }
            return rows;
        } catch (Exception e) {
            client.cancelScannerSubscription(reqId);
            throw new IbkrClientException("Scanner request failed", e);
        } finally {
            scannerFutures.remove(reqId);
            scannerBuffers.remove(reqId);
        }
    }

    /** Request historical bars for a contract. */
    public List<IbkrBar> requestHistoricalData(Contract contract,
                                               String endDateTime,
                                               String durationStr,
                                               String barSize,
                                               String whatToShow,
                                               boolean useRth,
                                               List<TagValue> options,
                                               Duration timeout) {
        Objects.requireNonNull(contract, "contract");
        ensureConnected();
        waitForPacingSlot();

        int reqId = nextRequestId();
        CompletableFuture<List<IbkrBar>> future = new CompletableFuture<>();
        histFutures.put(reqId, future);
        histBuffers.put(reqId, Collections.synchronizedList(new ArrayList<>()));

        log.debug("Requesting historical data for {} duration {} barSize {}", contract.symbol(), durationStr, barSize);
        client.reqHistoricalData(reqId, contract, endDateTime, durationStr, barSize, whatToShow, useRth ? 1 : 0, 1, false, options == null ? List.of() : options);

        try {
            long timeoutMillis = timeout != null ? timeout.toMillis() : properties.getDefaultRequestTimeoutMillis();
            List<IbkrBar> bars = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            bars.sort(Comparator.comparing(IbkrBar::time));
            return bars;
        } catch (Exception e) {
            client.cancelHistoricalData(reqId);
            throw new IbkrClientException("Historical data request failed", e);
        } finally {
            histFutures.remove(reqId);
            histBuffers.remove(reqId);
        }
    }

    @Override
    public void connectAck() {
        CountDownLatch latch = this.connectLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void nextValidId(int orderId) {
        CountDownLatch latch = this.connectLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
        List<IbkrScannerRow> buffer = scannerBuffers.get(reqId);
        if (buffer != null) {
            buffer.add(new IbkrScannerRow(rank, contractDetails, distance, benchmark, projection, legsStr));
        }
    }

    @Override
    public void scannerDataEnd(int reqId) {
        CompletableFuture<List<IbkrScannerRow>> future = scannerFutures.get(reqId);
        List<IbkrScannerRow> buffer = scannerBuffers.get(reqId);
        if (future != null) {
            future.complete(buffer != null ? List.copyOf(buffer) : List.of());
        }
    }

    @Override
    public void historicalData(int reqId, Bar bar) {
        List<IbkrBar> buffer = histBuffers.get(reqId);
        if (buffer != null) {
            buffer.add(new IbkrBar(parseTime(bar.time()), bar.open(), bar.high(), bar.low(), bar.close(), Optional.ofNullable(bar.volume()).map(v -> (long) v.longValue()).orElse(0L)));
        }
    }

    @Override
    public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
        CompletableFuture<List<IbkrBar>> future = histFutures.get(reqId);
        List<IbkrBar> buffer = histBuffers.get(reqId);
        if (future != null) {
            future.complete(buffer != null ? List.copyOf(buffer) : List.of());
        }
    }

    @Override
    public void error(int reqId, int errorCode, String errorMsg) {
        if (reqId >= 0) {
            CompletableFuture<List<IbkrScannerRow>> scannerFuture = scannerFutures.get(reqId);
            if (scannerFuture != null && !scannerFuture.isDone()) {
                scannerFuture.completeExceptionally(new IbkrClientException("Scanner error %d: %s".formatted(errorCode, errorMsg)));
            }
            CompletableFuture<List<IbkrBar>> histFuture = histFutures.get(reqId);
            if (histFuture != null && !histFuture.isDone()) {
                histFuture.completeExceptionally(new IbkrClientException("Historical error %d: %s".formatted(errorCode, errorMsg)));
            }
        }
        if (errorCode == 420 || errorCode == 421) { // pacing violations
            log.warn("IBKR pacing violation reported ({}). Applying backoff.", errorMsg);
            try {
                Thread.sleep(properties.getPacingViolationBackoffMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else if (errorCode >= 0) {
            log.warn("IBKR error {} for request {}: {}", errorCode, reqId, errorMsg);
        } else {
            log.warn("IBKR message: {}", errorMsg);
        }
    }

    @Override
    public void connectionClosed() {
        log.warn("IBKR connection closed");
        if (readerExecutor != null) {
            readerExecutor.shutdownNow();
            readerExecutor = null;
        }
        reader = null;
    }

    private Instant parseTime(String ibTime) {
        if (ibTime == null || ibTime.isBlank()) {
            return Instant.now();
        }
        String trimmed = ibTime.trim();
        boolean numeric = trimmed.chars().allMatch(Character::isDigit);
        if (numeric && trimmed.length() <= 10) {
            long epochSeconds = Long.parseLong(trimmed);
            return Instant.ofEpochSecond(epochSeconds);
        }
        String normalised = trimmed.replaceAll("\\s+", " ");
        if (normalised.length() == 8 && normalised.chars().allMatch(Character::isDigit)) {
            LocalDate date = LocalDate.parse(normalised, DateTimeFormatter.BASIC_ISO_DATE);
            return date.atStartOfDay(ZoneId.of("UTC")).toInstant();
        }
        LocalDateTime ldt = LocalDateTime.parse(normalised, IB_DATE_TIME);
        return ldt.atZone(ZoneId.of("UTC")).toInstant();
    }

    @Override
    public void destroy() {
        if (client.isConnected()) {
            client.eDisconnect();
        }
        if (readerExecutor != null) {
            readerExecutor.shutdownNow();
            readerExecutor = null;
        }
        reader = null;
    }

    /** Simple runtime exception used by the component. */
    public static class IbkrClientException extends RuntimeException {
        public IbkrClientException(String message) {
            super(message);
        }

        public IbkrClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Representation of a scanner row as returned by IBKR. */
    public record IbkrScannerRow(int rank, ContractDetails contractDetails, String distance, String benchmark,
                                 String projection, String legs) {}

    /** Simple immutable OHLC bar representation. */
    public record IbkrBar(Instant time, double open, double high, double low, double close, long volume) {}
}
