package finance.project.api.universe.service;

import finance.project.api.entities.Symbol;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.universe.Universe;
import finance.project.api.universe.UniverseRepository;
import finance.project.api.universe.UniverseType;
import finance.project.api.universe.client.CoingeckoUniverseClient;
import finance.project.api.universe.client.EodhdUniverseClient;
import finance.project.api.universe.dto.UniverseCatalogDTO;
import finance.project.api.universe.dto.UniverseDetailsDTO;
import finance.project.api.universe.dto.UniverseImportRequest;
import finance.project.api.universe.dto.UniverseImportResponse;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniverseService {

    private static final Logger log = LoggerFactory.getLogger(UniverseService.class);

    private static final Map<String, CatalogEntry> CATALOG;

    static {
        Map<String, CatalogEntry> entries = new LinkedHashMap<>();
        entries.put("SP500", new CatalogEntry("SP500", "S&P 500 (ETF SPY.US)", "EQUITY_ETF", "EODHD", 500, true, UniverseType.EQUITY, "SPY.US"));
        entries.put("NASDAQ100", new CatalogEntry("NASDAQ100", "Nasdaq 100 (ETF QQQ.US)", "EQUITY_ETF", "EODHD", 100, true, UniverseType.EQUITY, "QQQ.US"));
        entries.put("DOWJONES", new CatalogEntry("DOWJONES", "Dow Jones (ETF DIA.US)", "EQUITY_ETF", "EODHD", 30, true, UniverseType.EQUITY, "DIA.US"));
        entries.put("CAC40", new CatalogEntry("CAC40", "CAC 40 (ETF CAC.PA)", "EQUITY_ETF", "EODHD", 40, true, UniverseType.EQUITY, "CAC.PA"));
        entries.put("STOXX600", new CatalogEntry("STOXX600", "Stoxx Europe 600 (ETF EXSA.DE)", "EQUITY_ETF", "EODHD", 600, true, UniverseType.EQUITY, "EXSA.DE"));
        CATALOG = Map.copyOf(entries);
    }

    private record CatalogEntry(
            String code,
            String name,
            String catalogType,
            String provider,
            Integer approxSize,
            boolean importable,
            UniverseType universeType,
            String etfTicker
    ) {
    }

    private static final Set<String> KNOWN_EQUITY_SUFFIXES = Set.of(
            ".US",
            ".PA",
            ".DE",
            ".L",
            ".HK",
            ".SW",
            ".MI",
            ".F",
            ".CA",
            ".BR",
            ".SG",
            ".VX"
    );

    private final UniverseRepository universeRepository;
    private final SymbolRepository symbolRepository;
    private final CoingeckoUniverseClient coingeckoUniverseClient;
    private final EodhdUniverseClient eodhdUniverseClient;
    private final UniverseImportRunner universeImportRunner;

    public UniverseService(UniverseRepository universeRepository,
                           SymbolRepository symbolRepository,
                           CoingeckoUniverseClient coingeckoUniverseClient,
                           EodhdUniverseClient eodhdUniverseClient,
                           UniverseImportRunner universeImportRunner) {
        this.universeRepository = universeRepository;
        this.symbolRepository = symbolRepository;
        this.coingeckoUniverseClient = coingeckoUniverseClient;
        this.eodhdUniverseClient = eodhdUniverseClient;
        this.universeImportRunner = universeImportRunner;
    }

    public List<UniverseCatalogDTO> getCatalog() {
        return buildCatalogEntries().stream()
                .map(entry -> new UniverseCatalogDTO(
                        entry.code(),
                        entry.name(),
                        entry.catalogType(),
                        entry.provider(),
                        entry.approxSize(),
                        entry.importable()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UniverseDetailsDTO> getUniverseDetails(String code) {
        return universeRepository.findByCode(code)
                .map(universe -> new UniverseDetailsDTO(
                        universe.getId(),
                        universe.getCode(),
                        universe.getName(),
                        universe.getType().name(),
                        universe.getProvider(),
                        universe.getSymbols().stream().map(Symbol::getSymbol).sorted().toList()
                ));
    }

    @Transactional
    public UniverseImportResponse importUniverse(UniverseImportRequest request) {
        CatalogEntry entry = findCatalogEntry(request.code())
                .orElseThrow(() -> new IllegalArgumentException("Unknown universe code: " + request.code()));

        if (!entry.importable()) {
            throw new IllegalArgumentException("Universe is not importable: " + request.code());
        }
        if (!request.startDate().isBefore(request.endDate())) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }

        Universe universe = universeRepository.findByCode(entry.code())
                .orElseGet(Universe::new);

        universe.setCode(entry.code());
        universe.setName(entry.name());
        universe.setType(entry.universeType());
        universe.setProvider(entry.provider());

        Set<String> normalizedSymbols = loadSymbols(entry, request);
        log.info("[Universe] {} fetched {} symbols", entry.code(), normalizedSymbols.size());

        // Detach previous associations
        if (universe.getSymbols() != null && !universe.getSymbols().isEmpty()) {
            for (Symbol symbol : new HashSet<>(universe.getSymbols())) {
                symbol.getUniverses().remove(universe);
            }
            universe.getSymbols().clear();
        }

        for (String normalized : normalizedSymbols) {
            Symbol symbol = symbolRepository.findBySymbol(normalized)
                    .orElseGet(() -> createSymbol(normalized, entry.universeType()));
            universe.addSymbol(symbol);
            symbol.getUniverses().add(universe);
        }

        Universe saved = universeRepository.save(universe);
        universeImportRunner.runUniverseImport(saved.getId(), request);
        return new UniverseImportResponse(saved.getId(), saved.getCode(), saved.getType().name(), saved.getProvider());
    }

    private Symbol createSymbol(String symbolCode, UniverseType type) {
        Symbol symbol = Symbol.builder()
                .symbol(symbolCode)
                .name(symbolCode)
                .market(type.name())
                .build();
        return symbolRepository.save(symbol);
    }

    private Set<String> loadSymbols(CatalogEntry entry, UniverseImportRequest request) {
        List<String> rawSymbols;
        if (entry.universeType() == UniverseType.CRYPTO && "COINGECKO".equalsIgnoreCase(entry.provider())) {
            int limit = (entry.approxSize() != null && entry.approxSize() > 0)
                    ? entry.approxSize()
                    : 100; // fallback par défaut

            if (entry.approxSize() == null || entry.approxSize() <= 0) {
                log.warn("[UniverseImport] No approxSize defined for universe {}, using default {}", entry.code(), limit);
            }

            // 🔥 Si c'est un univers de type catégorie Coingecko (ex: ai-agents)
            if ("CRYPTO_CATEGORY".equalsIgnoreCase(entry.catalogType())) {
                rawSymbols = coingeckoUniverseClient.fetchCategorySymbols(entry.code(), limit);
            } else {
                // fallback : top du marché global
                rawSymbols = coingeckoUniverseClient.fetchTopCryptoSymbols(limit);
            }

        } else if (entry.universeType() == UniverseType.EQUITY
                && "EODHD".equalsIgnoreCase(entry.provider())) {
            rawSymbols = fetchEtfSymbols(entry);
        } else {
            rawSymbols = List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : rawSymbols) {
            String norm = normalizeSymbol(raw, entry.universeType(), request.broker());
            if (norm != null && !norm.isBlank()) {
                normalized.add(norm);
            }
        }
        return normalized;
    }

    private String normalizeSymbol(String raw, UniverseType type, String broker) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String symbol = raw.trim().toUpperCase(Locale.ROOT);
        if (type == UniverseType.CRYPTO) {
            if (symbol.contains("/")) {
                symbol = symbol.replace("/", "");
            }
            if (!symbol.endsWith("USD") && !symbol.endsWith("USDT")) {
                symbol = symbol + ("BINANCE".equalsIgnoreCase(broker) ? "USDT" : "USD");
            }
        } else if (type == UniverseType.EQUITY) {
            symbol = normalizeEquitySymbol(symbol);
        }
        return symbol;
    }

    private List<CatalogEntry> buildCatalogEntries() {
        List<CatalogEntry> catalog = new ArrayList<>();

        try {
            List<CoingeckoUniverseClient.CoinCategory> categories = coingeckoUniverseClient.listCategories();
            for (CoingeckoUniverseClient.CoinCategory cat : categories) {
                if (cat == null || cat.category_id() == null || cat.category_id().isBlank()) {
                    continue;
                }
                catalog.add(buildCryptoCategoryEntry(cat));
            }
        } catch (Exception e) {
            log.warn("[UniverseCatalog] Failed to load CoinGecko categories", e);
        }

        try {
            List<EodhdUniverseClient.EodhdEtfUniverse> etfUniverses = eodhdUniverseClient.listSupportedEtfUniverses();
            for (EodhdUniverseClient.EodhdEtfUniverse etf : etfUniverses) {
                if (etf == null || etf.code() == null || etf.code().isBlank()) {
                    continue;
                }
                catalog.add(buildEtfUniverseEntry(etf));
            }
        } catch (Exception e) {
            log.warn("[UniverseCatalog] Failed to load EODHD ETF universes", e);
            log.info("[UniverseCatalog] Using static fallback catalog. ");
            catalog.addAll(CATALOG.values());
            return catalog;
        }

        return catalog;
    }

    private Optional<CatalogEntry> findCatalogEntry(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        List<CatalogEntry> catalog = buildCatalogEntries();

        Optional<CatalogEntry> direct = catalog.stream()
                .filter(entry -> entry.code() != null && entry.code().equalsIgnoreCase(normalized))
                .findFirst();
        if (direct.isPresent()) {
            return direct;
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        return catalog.stream()
                .filter(entry -> matchesAlias(upper, entry))
                .findFirst();
    }

    private boolean matchesAlias(String requestedUpper, CatalogEntry entry) {
        if (entry.code() == null) {
            return false;
        }

        String entryCodeUpper = entry.code().toUpperCase(Locale.ROOT);
        if (requestedUpper.equals(entryCodeUpper)) {
            return true;
        }
        if (entry.etfTicker() != null && requestedUpper.equals(entry.etfTicker().toUpperCase(Locale.ROOT))) {
            return true;
        }
        if (!entry.importable()) {
            return false;
        }

        String entryNameUpper = entry.name() != null ? entry.name().toUpperCase(Locale.ROOT) : "";

        return switch (requestedUpper) {
            case "SP500", "S&P500", "SPX", "SPY", "SPY.US" -> entryCodeUpper.contains("SP500")
                    || entryNameUpper.contains("S&P 500");
            case "NASDAQ100", "NASDAQ", "NDX", "QQQ", "QQQ.US" -> entryCodeUpper.contains("NASDAQ100")
                    || entryNameUpper.contains("NASDAQ");
            case "DOW", "DOWJONES", "DJI", "DIA", "DIA.US" -> entryCodeUpper.contains("DOWJONES")
                    || entryNameUpper.contains("DOW JONES")
                    || entryNameUpper.contains("DOW");
            case "CAC40", "CAC", "CAC.PA" -> entryCodeUpper.contains("CAC40")
                    || entryNameUpper.contains("CAC 40");
            case "STOXX600", "STOXX", "EXSA", "EXSA.DE" -> entryCodeUpper.contains("STOXX600")
                    || entryNameUpper.contains("STOXX");
            default -> false;
        };
    }

    private CatalogEntry buildCryptoCategoryEntry(CoingeckoUniverseClient.CoinCategory cat) {
        String code = cat.category_id();
        String name = (cat.name() != null && !cat.name().isBlank()) ? cat.name() : code;
        return new CatalogEntry(code, name, "CRYPTO_CATEGORY", "COINGECKO", null, true, UniverseType.CRYPTO, null);
    }

    private CatalogEntry buildEtfUniverseEntry(EodhdUniverseClient.EodhdEtfUniverse etf) {
        String code = etf.code();
        String name = (etf.name() != null && !etf.name().isBlank()) ? etf.name() : code;
        String catalogType = etf.type() != null ? etf.type() : "EQUITY_ETF";
        String provider = etf.provider() != null ? etf.provider() : "EODHD";
        return new CatalogEntry(code, name, catalogType, provider, etf.approxSize(), true, UniverseType.EQUITY, etf.etfTicker());
    }

    private List<String> fetchEtfSymbols(CatalogEntry entry) {
        if (entry.etfTicker() == null || entry.etfTicker().isBlank()) {
            log.warn("[UniverseImport] No ETF ticker configured for universe {}", entry.code());
            return List.of();
        }
        List<EodhdUniverseClient.EodhdHolding> holdings = eodhdUniverseClient.fetchEtfHoldings(entry.etfTicker());
        if (holdings.isEmpty()) {
            log.warn("[UniverseImport] No holdings returned for universe {} (ticker {})", entry.code(), entry.etfTicker());
            return List.of();
        }
        List<String> symbols = new ArrayList<>();
        for (EodhdUniverseClient.EodhdHolding holding : holdings) {
            if (holding == null || holding.code() == null || holding.code().isBlank()) {
                continue;
            }
            symbols.add(holding.code());
        }
        return symbols;
    }

    private String normalizeEquitySymbol(String symbol) {
        for (String suffix : KNOWN_EQUITY_SUFFIXES) {
            if (symbol.endsWith(suffix)) {
                return symbol.substring(0, symbol.length() - suffix.length());
            }
        }
        return symbol;
    }
}
