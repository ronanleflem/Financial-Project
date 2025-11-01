package finance.project.api.controllers;

import finance.project.api.services.IbkrFxService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/ibkr")
public class IbkrController {

    private final IbkrFxService ib;

    public IbkrController(IbkrFxService ib) {
        this.ib = ib;
    }

    // --- Santé / statut ---
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "connected", ib.isConnected()
        );
    }

    // --- Connexion / Déconnexion ---
    @PostMapping("/connect")
    public Map<String, Object> connect(
            @RequestParam(defaultValue = "127.0.0.1") String host,
            @RequestParam(defaultValue = "7497") int port,
            @RequestParam(defaultValue = "1") int clientId
    ) {
        ib.connect(host, port, clientId);
        return Map.of(
                "connected", ib.isConnected(),
                "host", host,
                "port", port,
                "clientId", clientId
        );
    }

    @PostMapping("/connect/wait")
    public Map<String,Object> connectAndWait(
            @RequestParam(defaultValue="127.0.0.1") String host,
            @RequestParam(defaultValue="7497") int port,
            @RequestParam(defaultValue="1") int clientId,
            @RequestParam(defaultValue="3000") long timeoutMs
    ) {
        boolean ok = ib.connectAndWait(host, port, clientId, timeoutMs);
        return Map.of(
                "connected", ok,
                "host", host,
                "port", port,
                "clientId", clientId,
                "timeoutMs", timeoutMs
        );
    }

    @GetMapping("/diag")
    public Map<String, Object> diag() {
        return finance.project.api.bootstrap.IbkrRuntimeDiag.collect();
    }

    @PostMapping("/market-data-type")
    public Map<String, Object> setMarketDataType(@RequestParam int type) {
        ensureConnected();
        ib.setMarketDataType(type);
        return Map.of(
                "ok", true,
                "marketDataType", type,
                "hint", "1=live,2=frozen,3=delayed,4=delayed-frozen"
        );
    }

    @PostMapping("/disconnect")
    public Map<String, Object> disconnect() {
        ib.disconnect();
        return Map.of("connected", ib.isConnected());
    }

    // --- LIVE EUR/USD ---
    /** Démarre le flux Level 1 EURUSD (BID/ASK). Retourne le requestId. */
    @PostMapping("/live/eurusd/start")
    public Map<String, Object> startLiveEurUsd() {
        ensureConnected();
        int reqId = ib.startLiveEurUsd();
        return Map.of("reqId", reqId, "message", "EURUSD live started");
    }

    /** Stoppe un flux live à partir du requestId. */
    @PostMapping("/live/stop/{reqId}")
    public Map<String, Object> stopLive(@PathVariable int reqId) {
        ensureConnected();
        ib.stopLive(reqId);
        return Map.of("reqId", reqId, "message", "live stopped");
    }

    /** Récupère un snapshot (FIFO) des derniers ticks live bufferisés. */
    @GetMapping("/live/eurusd")
    public List<IbkrFxService.FxQuote> recentLiveQuotes() {
        return ib.getRecentLiveQuotes();
    }

    // ---- LIVE BARS 1m EUR/USD ----

    /** Démarre un flux de bougies 1 minute EURUSD (MIDPOINT). Retourne reqId. */
    @PostMapping("/live/eurusd/1m/start")
    public Map<String, Object> startLiveEurUsd1m() {
        ensureConnected();
        int reqId = ib.startLiveMinuteBarsEurUsd();
        return Map.of("reqId", reqId, "message", "EURUSD 1m bars live started");
    }

    @PostMapping("/live/bars/start")
    public Map<String,Object> startLiveBars(
            @RequestParam(defaultValue = "EURUSD") String pair,
            @RequestParam(defaultValue = "1 D") String duration,
            @RequestParam(defaultValue = "1 min") String barSize,
            @RequestParam(defaultValue = "MIDPOINT") String what,
            @RequestParam(defaultValue = "0") int rth,
            @RequestParam(defaultValue = "2") int formatDate  // 2 = epoch recommandé
    ) {
        ensureConnected();
        int reqId = ib.startLiveBars(pair, duration, barSize, what, rth, formatDate);
        return Map.of("reqId", reqId,
                "pair", pair, "duration", duration, "barSize", barSize, "what", what, "rth", rth, "formatDate", formatDate);
    }

    /** Récupère les dernières bougies pour un flux donné (par reqId). */
    @GetMapping("/live/bars/{reqId}")
    public List<IbkrFxService.HistBar> recentLiveBars(@PathVariable int reqId) {
        return ib.getRecentLiveBars(reqId);
    }

    @GetMapping("/live/bars/{reqId}/last")
    public IbkrFxService.HistBar getLastLiveBar(@PathVariable int reqId) {
        return ib.getLastLiveBar(reqId);
    }



    /** Stoppe le flux de bougies (par reqId). */
    @PostMapping("/live/bars/stop/{reqId}")
    public Map<String, Object> stopLiveBars(@PathVariable int reqId) {
        ensureConnected();
        ib.stopLiveBars(reqId);
        return Map.of("reqId", reqId, "stopped", true);
    }

    // --- HISTORIQUE EUR/USD ---
    /**
     * Exemple : /ibkr/hist/eurusd?duration=1%20D&barSize=1%20min
     * duration : "1 D", "1 W", "1 M", ...
     * barSize : "1 min", "5 mins", "1 hour", ...
     */
    @GetMapping("/hist/eurusd")
    public List<IbkrFxService.HistBar> histEurUsd(
            @RequestParam(defaultValue = "1 D") String duration,
            @RequestParam(defaultValue = "1 min") String barSize
    ) {
        ensureConnected();
        try {
            return ib.getHistoricalEurUsd(duration, barSize);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IBKR historical fetch failed: " + e.getMessage(), e);
        }
    }

    @GetMapping("/portfolio")
    public IbkrFxService.PortfolioSnapshot getPortfolio(
            @RequestParam(defaultValue = "IBKR") String broker,
            @RequestParam(required = false) String account,
            @RequestParam(defaultValue = "5000") long timeoutMs
    ) {
        if (!"IBKR".equalsIgnoreCase(broker)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Broker non supporté: " + broker);
        }
        ensureConnected();
        try {
            return ib.getPortfolioSnapshot(account, timeoutMs);
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "IBKR portfolio timeout: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            Throwable root = e.getCause() != null ? e.getCause() : e;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IBKR portfolio error: " + root.getMessage(), root);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Thread interrupted", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, e.getMessage(), e);
        }
    }

    // --- Helper ---
    private void ensureConnected() {
        if (!ib.isConnected()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Not connected to TWS/IB Gateway");
        }
    }

    // (Optionnel) handler générique pour des erreurs non-captées
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> onError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> onAnyError(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return ResponseEntity.status(500).body(Map.of(
                "error", t.getClass().getName(),
                "message", String.valueOf(t.getMessage()),
                "rootError", root.getClass().getName(),
                "rootMessage", String.valueOf(root.getMessage())
        ));
    }
}