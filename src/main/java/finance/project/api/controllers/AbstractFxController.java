package finance.project.api.controllers;

import finance.project.api.model.fx.FxQuote;
import finance.project.api.model.fx.HistBar;
import finance.project.api.services.FxMarketDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Base REST controller exposing a common surface for FX market data providers.
 */
@ResponseBody
public abstract class AbstractFxController {

    private final FxMarketDataService service;

    protected AbstractFxController(FxMarketDataService service) {
        this.service = service;
    }

    protected FxMarketDataService getService() {
        return service;
    }

    // --- Santé / statut ---
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "provider", service.getProvider(),
                "connected", service.isConnected()
        );
    }

    // --- Connexion / Déconnexion ---
    @PostMapping("/connect")
    public Map<String, Object> connect(
            @RequestParam(defaultValue = "127.0.0.1") String host,
            @RequestParam(defaultValue = "7497") int port,
            @RequestParam(defaultValue = "1") int clientId
    ) {
        service.connect(host, port, clientId);
        return Map.of(
                "provider", service.getProvider(),
                "connected", service.isConnected(),
                "host", host,
                "port", port,
                "clientId", clientId
        );
    }

    @PostMapping("/connect/wait")
    public Map<String, Object> connectAndWait(
            @RequestParam(defaultValue = "127.0.0.1") String host,
            @RequestParam(defaultValue = "7497") int port,
            @RequestParam(defaultValue = "1") int clientId,
            @RequestParam(defaultValue = "3000") long timeoutMs
    ) {
        boolean ok = service.connectAndWait(host, port, clientId, timeoutMs);
        return Map.of(
                "provider", service.getProvider(),
                "connected", ok,
                "host", host,
                "port", port,
                "clientId", clientId,
                "timeoutMs", timeoutMs
        );
    }

    @PostMapping("/market-data-type")
    public Map<String, Object> setMarketDataType(@RequestParam int type) {
        ensureConnected();
        if (!service.supportsMarketDataType()) {
            return Map.of(
                    "provider", service.getProvider(),
                    "ok", false,
                    "message", "Market data type switching is not supported for this provider"
            );
        }
        service.setMarketDataType(type);
        return Map.of(
                "provider", service.getProvider(),
                "ok", true,
                "marketDataType", type,
                "hint", "1=live,2=frozen,3=delayed,4=delayed-frozen"
        );
    }

    @PostMapping("/disconnect")
    public Map<String, Object> disconnect() {
        service.disconnect();
        return Map.of(
                "provider", service.getProvider(),
                "connected", service.isConnected()
        );
    }

    // --- LIVE EUR/USD ---
    @PostMapping("/live/eurusd/start")
    public Map<String, Object> startLiveEurUsd() {
        ensureConnected();
        int reqId = service.startLiveEurUsd();
        return Map.of(
                "provider", service.getProvider(),
                "reqId", reqId,
                "message", "EURUSD live started"
        );
    }

    @PostMapping("/live/stop/{reqId}")
    public Map<String, Object> stopLive(@PathVariable int reqId) {
        ensureConnected();
        service.stopLive(reqId);
        return Map.of(
                "provider", service.getProvider(),
                "reqId", reqId,
                "message", "live stopped"
        );
    }

    @GetMapping("/live/eurusd")
    public List<FxQuote> recentLiveQuotes() {
        return service.getRecentLiveQuotes();
    }

    // ---- LIVE BARS 1m EUR/USD ----
    @PostMapping("/live/eurusd/1m/start")
    public Map<String, Object> startLiveEurUsd1m() {
        ensureConnected();
        int reqId = service.startLiveMinuteBarsEurUsd();
        return Map.of(
                "provider", service.getProvider(),
                "reqId", reqId,
                "message", "EURUSD 1m bars live started"
        );
    }

    @PostMapping("/live/bars/start")
    public Map<String, Object> startLiveBars(
            @RequestParam(defaultValue = "EURUSD") String pair,
            @RequestParam(defaultValue = "1 D") String duration,
            @RequestParam(defaultValue = "1 min") String barSize,
            @RequestParam(defaultValue = "MIDPOINT") String what,
            @RequestParam(defaultValue = "0") int rth,
            @RequestParam(defaultValue = "2") int formatDate
    ) {
        ensureConnected();
        int reqId = service.startLiveBars(pair, duration, barSize, what, rth, formatDate);
        return Map.of(
                "provider", service.getProvider(),
                "reqId", reqId,
                "pair", pair,
                "duration", duration,
                "barSize", barSize,
                "what", what,
                "rth", rth,
                "formatDate", formatDate
        );
    }

    @GetMapping("/live/bars/{reqId}")
    public List<HistBar> recentLiveBars(@PathVariable int reqId) {
        return service.getRecentLiveBars(reqId);
    }

    @GetMapping("/live/bars/{reqId}/last")
    public HistBar getLastLiveBar(@PathVariable int reqId) {
        return service.getLastLiveBar(reqId);
    }

    @PostMapping("/live/bars/stop/{reqId}")
    public Map<String, Object> stopLiveBars(@PathVariable int reqId) {
        ensureConnected();
        service.stopLiveBars(reqId);
        return Map.of(
                "provider", service.getProvider(),
                "reqId", reqId,
                "stopped", true
        );
    }

    // --- HISTORIQUE EUR/USD ---
    @GetMapping("/hist/eurusd")
    public List<HistBar> histEurUsd(
            @RequestParam(defaultValue = "1 D") String duration,
            @RequestParam(defaultValue = "1 min") String barSize
    ) {
        ensureConnected();
        try {
            return service.getHistoricalEurUsd(duration, barSize);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    service.getProvider().toUpperCase() + " historical fetch failed: " + e.getMessage(), e);
        }
    }

    protected void ensureConnected() {
        if (!service.isConnected()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "Not connected to provider " + service.getProvider());
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> onError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "provider", service.getProvider(),
                        "error", e.getClass().getSimpleName(),
                        "message", e.getMessage()
                ));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> onAnyError(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return ResponseEntity.status(500).body(Map.of(
                "provider", service.getProvider(),
                "error", t.getClass().getName(),
                "message", String.valueOf(t.getMessage()),
                "rootError", root.getClass().getName(),
                "rootMessage", String.valueOf(root.getMessage())
        ));
    }
}
