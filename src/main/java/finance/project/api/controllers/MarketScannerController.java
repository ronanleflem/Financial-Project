package finance.project.api.controllers;

import finance.project.api.ibkr.IbkrRequestException;
import finance.project.api.model.market.MarketScanItem;
import finance.project.api.model.market.OhlcBar;
import finance.project.api.model.market.ScannerUniversesResponse;
import finance.project.api.services.market.HistoricalDataService;
import finance.project.api.services.market.MarketScannerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketScannerController {

    private final MarketScannerService marketScannerService;
    private final HistoricalDataService historicalDataService;

    public MarketScannerController(MarketScannerService marketScannerService,
                                   HistoricalDataService historicalDataService) {
        this.marketScannerService = marketScannerService;
        this.historicalDataService = historicalDataService;
    }

    @GetMapping("/scans/top-losers")
    public List<MarketScanItem> scanTopLosers(@RequestParam String region,
                                              @RequestParam(required = false) String assetClass,
                                              @RequestParam(required = false) Double minMarketCap,
                                              @RequestParam(defaultValue = "20") int limit) {
        return marketScannerService.scanTopLosersByRegion(region, assetClass, minMarketCap, limit);
    }

    @GetMapping("/scans/top-off-high")
    public List<MarketScanItem> scanTopOffHigh(@RequestParam String region,
                                               @RequestParam(required = false) String assetClass,
                                               @RequestParam(required = false) Double minMarketCap,
                                               @RequestParam(defaultValue = "20") int limit) {
        return marketScannerService.scanTopOffHighByRegion(region, assetClass, minMarketCap, limit);
    }

    @GetMapping("/scans/index")
    public List<MarketScanItem> scanByIndex(@RequestParam String indexCode,
                                            @RequestParam(required = false) String scanType,
                                            @RequestParam(defaultValue = "20") int limit) {
        return marketScannerService.scanByIndexConstituents(indexCode, scanType, limit);
    }

    @GetMapping("/scans/high-volume-drops")
    public List<MarketScanItem> scanHighVolumeDrops(@RequestParam("regionOrIndex") String regionOrIndex,
                                                    @RequestParam(required = false) Double minMarketCap,
                                                    @RequestParam(defaultValue = "20") int limit) {
        return marketScannerService.scanHighVolumeDropsByRegionOrIndex(regionOrIndex, minMarketCap, limit);
    }

    @GetMapping("/universes")
    public ScannerUniversesResponse getUniverses() {
        return marketScannerService.getScannerUniverses();
    }

    @GetMapping("/ohlc")
    public List<OhlcBar> getOhlc(@RequestParam String symbol,
                                 @RequestParam(required = false) String assetClass,
                                 @RequestParam(required = false) String start,
                                 @RequestParam(required = false) String end,
                                 @RequestParam(required = false) String timeframe) {
        Instant startInstant = parseInstant(start);
        Instant endInstant = parseInstant(end);
        return historicalDataService.getHistoricalOhlc(symbol, assetClass, startInstant, endInstant, timeframe);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IbkrRequestException.class)
    public ResponseEntity<Map<String, String>> handleIbkr(IbkrRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateParse(DateTimeParseException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use ISO-8601."));
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
