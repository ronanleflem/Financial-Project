package finance.project.api.services.market;

import com.ib.client.Contract;
import com.ib.client.ScannerSubscription;
import com.ib.client.TagValue;
import finance.project.api.ibkr.IbkrRequestException;
import finance.project.api.ibkr.model.IbkrBar;
import finance.project.api.ibkr.model.IbkrScannerRow;
import finance.project.api.model.market.MarketScanItem;
import finance.project.api.model.market.ScannerUniversesResponse;
import finance.project.api.services.IbkrFxService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class MarketScannerService {

    private static final Logger log = LoggerFactory.getLogger(MarketScannerService.class);

    private static final Map<String, String> REGION_LOCATIONS = Map.ofEntries(
            Map.entry("US", "STK.US.MAJOR"),
            Map.entry("NORTH_AMERICA", "STK.NA"),
            Map.entry("EUROPE", "STK.EUROPE"),
            Map.entry("UK", "STK.LONDON"),
            Map.entry("GERMANY", "STK.DEX"),
            Map.entry("FRANCE", "STK.PARIS"),
            Map.entry("ASIA", "STK.ASIA"),
            Map.entry("JAPAN", "STK.JAPAN"),
            Map.entry("HONG_KONG", "STK.HKSE")
    );

    private final Map<String, IndexDefinition> indices = new HashMap<>();

    private final IbkrFxService ibkrService;

    public MarketScannerService(IbkrFxService ibkrService) {
        this.ibkrService = ibkrService;
    }

    @PostConstruct
    void init() {
        indices.put("SP500", new IndexDefinition(
                "SP500",
                "S&P 500 Megacaps",
                "USD",
                "SMART",
                "STK.US.MAJOR",
                List.of("AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "BRK B", "JPM", "JNJ", "XOM")));
        indices.put("CAC40", new IndexDefinition(
                "CAC40",
                "CAC 40",
                "EUR",
                "SMART",
                "STK.PARIS",
                List.of("OR", "MC", "AI", "BNP", "AIR", "DG", "SAN", "BN", "RMS", "GLE")));
        indices.put("DAX40", new IndexDefinition(
                "DAX40",
                "DAX 40",
                "EUR",
                "SMART",
                "STK.DEX",
                List.of("SAP", "SIE", "IFX", "ALV", "DTE", "BMW", "BAYN", "VOW3", "MUV2", "RWE")));
        indices.put("STOXX600", new IndexDefinition(
                "STOXX600",
                "STOXX Europe 600",
                "EUR",
                "SMART",
                "STK.EUROPE",
                List.of("NESN", "ASML", "NOVN", "ROG", "SAP", "MC", "AIR", "OR", "HSBA", "BP")));
    }

    public List<MarketScanItem> scanTopLosersByRegion(String region, String assetClass, Double minMarketCap, int limit) {
        String locationCode = resolveLocation(region);
        ScannerSubscription subscription = buildBaseSubscription(locationCode, mapAssetClass(assetClass), "TOP_PERC_LOSERS", limit);
        List<TagValue> filters = buildMarketCapFilter(minMarketCap);
        List<IbkrScannerRow> rows = ibkrService.requestScannerData(subscription, limit, Duration.ofSeconds(60), filters);
        return rows.stream()
                .map(row -> toMarketScanItem(row, valueToDouble(row.distance()), null, null))
                .filter(item -> item.changePct() == null || item.changePct() < 0)
                .limit(limit > 0 ? limit : rows.size())
                .toList();
    }

    public List<MarketScanItem> scanTopOffHighByRegion(String region, String assetClass, Double minMarketCap, int limit) {
        String locationCode = resolveLocation(region);
        ScannerSubscription subscription = buildBaseSubscription(locationCode, mapAssetClass(assetClass), "TOP_PERC_OFF_HIGH", limit);
        List<TagValue> filters = buildMarketCapFilter(minMarketCap);
        List<IbkrScannerRow> rows = ibkrService.requestScannerData(subscription, limit, Duration.ofSeconds(30), filters);
        return rows.stream()
                .map(row -> toMarketScanItem(row, null, null, valueToDouble(row.distance())))
                .limit(limit > 0 ? limit : rows.size())
                .toList();
    }

    public List<MarketScanItem> scanByIndexConstituents(String indexCode, String scanType, int limit) {
        IndexDefinition index = indices.get(normaliseKey(indexCode));
        if (index == null) {
            throw new IllegalArgumentException("Unknown index: " + indexCode);
        }
        List<String> tickers = new ArrayList<>(index.tickers());
        if (limit > 0 && limit < tickers.size()) {
            tickers = tickers.subList(0, limit);
        }
        List<MarketScanItem> results = new CopyOnWriteArrayList<>();
        for (String ticker : tickers) {
            Contract contract = buildStockContract(ticker, index.currency(), index.primaryExchange());
            List<IbkrBar> bars;
            try {
                bars = ibkrService.requestHistoricalData(contract, "", "2 D", "1 day", "TRADES", true, List.of(), Duration.ofSeconds(8));
            } catch (IbkrRequestException ex) {
                log.warn("Unable to retrieve historical data for {}: {}", ticker, ex.getMessage());
                continue;
            }
            if (bars.isEmpty()) {
                continue;
            }
            double last = bars.get(bars.size() - 1).close();
            Double changePct = null;
            if (bars.size() >= 2) {
                double previous = bars.get(bars.size() - 2).close();
                if (previous != 0) {
                    changePct = ((last - previous) / previous) * 100.0;
                }
            }
            results.add(new MarketScanItem(
                    ticker,
                    ticker,
                    index.primaryExchange(),
                    last,
                    changePct,
                    null,
                    index.currency(),
                    null,
                    null,
                    null
            ));
        }
        Comparator<MarketScanItem> comparator = Comparator.comparing(item -> Optional.ofNullable(item.changePct()).orElse(0.0));
        if (scanType != null) {
            switch (scanType.toUpperCase(Locale.ROOT)) {
                case "CHANGE_DESC" -> comparator = comparator.reversed();
                case "CHANGE_ASC" -> {
                    // default order already ascending
                }
                default -> {
                    // ignore unsupported scan type, keep default order
                }
            }
        }
        return results.stream().sorted(comparator).collect(Collectors.toList());
    }

    public List<MarketScanItem> scanHighVolumeDropsByRegionOrIndex(String regionOrIndex, Double minMarketCap, int limit) {
        String key = normaliseKey(regionOrIndex);
        List<TagValue> filters = buildMarketCapFilter(minMarketCap);
        if (REGION_LOCATIONS.containsKey(key)) {
            int fetchLimit = limit > 0 ? Math.max(limit * 2, limit) : 50;
            ScannerSubscription subscription = buildBaseSubscription(REGION_LOCATIONS.get(key), "STK", "HOT_BY_VOLUME", fetchLimit);
            List<IbkrScannerRow> rows = ibkrService.requestScannerData(subscription, fetchLimit, Duration.ofSeconds(30), filters);
            return rows.stream()
                    .map(row -> toMarketScanItem(row, valueToDouble(row.distance()), null, null))
                    .filter(item -> item.changePct() != null && item.changePct() < 0)
                    .limit(limit > 0 ? limit : rows.size())
                    .toList();
        }
        IndexDefinition index = indices.get(key);
        if (index != null) {
            int fetchLimit = limit > 0 ? Math.max(limit * 3, limit) : 60;
            ScannerSubscription subscription = buildBaseSubscription(index.locationCode(), "STK", "HOT_BY_VOLUME", fetchLimit);
            List<IbkrScannerRow> rows = ibkrService.requestScannerData(subscription, fetchLimit, Duration.ofSeconds(30), filters);
            Set<String> tickers = index.tickers().stream().map(this::normaliseKey).collect(Collectors.toSet());
            return rows.stream()
                    .filter(row -> tickers.contains(normaliseKey(row.contractDetails().contract().symbol())))
                    .map(row -> toMarketScanItem(row, valueToDouble(row.distance()), null, null))
                    .filter(item -> item.changePct() != null && item.changePct() < 0)
                    .limit(limit > 0 ? limit : rows.size())
                    .toList();
        }
        throw new IllegalArgumentException("Unknown region or index: " + regionOrIndex);
    }

    public ScannerUniversesResponse getScannerUniverses() {
        Map<String, String> regions = new LinkedHashMap<>();
        REGION_LOCATIONS.forEach((key, value) -> regions.put(key, value));

        Map<String, ScannerUniversesResponse.IndexUniverse> indexMap = new LinkedHashMap<>();
        indices.values().forEach(idx -> indexMap.put(idx.code(), new ScannerUniversesResponse.IndexUniverse(
                idx.code(),
                idx.description(),
                idx.currency(),
                List.copyOf(idx.tickers()))));

        Map<String, Double> minCaps = Map.of(
                "US", 5_000_000_000d,
                "EUROPE", 3_000_000_000d,
                "UK", 2_000_000_000d,
                "GERMANY", 2_000_000_000d,
                "FRANCE", 2_000_000_000d,
                "ASIA", 2_000_000_000d
        );
        return new ScannerUniversesResponse(regions, indexMap, minCaps);
    }

    private List<TagValue> buildMarketCapFilter(Double minMarketCap) {
        if (minMarketCap == null || minMarketCap <= 0) {
            return List.of();
        }
        return List.of(new TagValue("marketCapAbove", Long.toString(minMarketCap.longValue())));
    }

    private ScannerSubscription buildBaseSubscription(String locationCode, String instrument, String scanCode, int limit) {
        ScannerSubscription subscription = new ScannerSubscription();
        subscription.locationCode(locationCode);
        subscription.instrument(instrument);
        subscription.scanCode(scanCode);
        if (limit > 0) {
            subscription.numberOfRows(limit);
        }
        return subscription;
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

    private MarketScanItem toMarketScanItem(IbkrScannerRow row, Double changePct, Double marketCap, Double offHighPct) {
        String exchange = Optional.ofNullable(row.contractDetails().contract().primaryExch())
                .filter(s -> !s.isBlank())
                .orElse(row.contractDetails().contract().exchange());
        String name = Optional.ofNullable(row.contractDetails().longName())
                .filter(s -> !s.isBlank())
                .orElse(row.contractDetails().contract().localSymbol());
        Double last = valueToDouble(row.benchmark());
        Double volumeRatio = valueToDouble(row.projection());
        Double computedChange = changePct != null ? changePct : valueToDouble(row.distance());
        return new MarketScanItem(
                row.contractDetails().contract().symbol(),
                name,
                exchange,
                last,
                computedChange,
                marketCap,
                row.contractDetails().contract().currency(),
                row.contractDetails().industry(),
                offHighPct,
                volumeRatio
        );
    }

    private String resolveLocation(String region) {
        String key = normaliseKey(region);
        String code = REGION_LOCATIONS.get(key);
        if (code == null) {
            throw new IllegalArgumentException("Unsupported region: " + region);
        }
        return code;
    }

    private String normaliseKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private Contract buildStockContract(String symbol, String currency, String primaryExchange) {
        Contract contract = new Contract();
        contract.symbol(symbol);
        contract.secType("STK");
        contract.currency(currency);
        contract.exchange("SMART");
        if (primaryExchange != null && !primaryExchange.isBlank()) {
            contract.primaryExch(primaryExchange);
        }
        return contract;
    }

    private Double valueToDouble(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace("%", "").replace(",", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record IndexDefinition(String code, String description, String currency, String primaryExchange,
                                   String locationCode, List<String> tickers) {
    }
}
