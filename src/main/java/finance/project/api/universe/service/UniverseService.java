package finance.project.api.universe.service;

import finance.project.api.entities.Symbol;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.universe.Universe;
import finance.project.api.universe.UniverseRepository;
import finance.project.api.universe.UniverseType;
import finance.project.api.universe.client.CoingeckoUniverseClient;
import finance.project.api.universe.csv.CsvUniverseLoader;
import finance.project.api.universe.csv.CsvUniverseLoader.CsvUniverseDefinition;
import finance.project.api.universe.csv.CsvUniverseLoader.CsvUniverseSymbol;
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

    private record CatalogEntry(
            String code,
            String name,
            String catalogType,
            String provider,
            Integer approxSize,
            boolean importable,
            UniverseType universeType
    ) {
    }

    private final UniverseRepository universeRepository;
    private final SymbolRepository symbolRepository;
    private final CoingeckoUniverseClient coingeckoUniverseClient;
    private final CsvUniverseLoader csvUniverseLoader;
    private final UniverseImportRunner universeImportRunner;

    public UniverseService(UniverseRepository universeRepository,
                           SymbolRepository symbolRepository,
                           CoingeckoUniverseClient coingeckoUniverseClient,
                           CsvUniverseLoader csvUniverseLoader,
                           UniverseImportRunner universeImportRunner) {
        this.universeRepository = universeRepository;
        this.symbolRepository = symbolRepository;
        this.coingeckoUniverseClient = coingeckoUniverseClient;
        this.csvUniverseLoader = csvUniverseLoader;
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

        Set<Symbol> symbolsToAssociate = loadSymbols(entry, request);
        log.info("[Universe] {} fetched {} symbols", entry.code(), symbolsToAssociate.size());

        // Detach previous associations
        if (universe.getSymbols() != null && !universe.getSymbols().isEmpty()) {
            for (Symbol symbol : new HashSet<>(universe.getSymbols())) {
                symbol.getUniverses().remove(universe);
            }
            universe.getSymbols().clear();
        }

        for (Symbol symbol : symbolsToAssociate) {
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

    private Symbol createOrUpdateSymbolFromCsv(CsvUniverseSymbol csvSymbol, UniverseType universeType) {
        if (csvSymbol == null || csvSymbol.symbol() == null || csvSymbol.symbol().isBlank()) {
            return null;
        }
        String normalized = csvSymbol.symbol().trim().toUpperCase(Locale.ROOT);
        String name = (csvSymbol.name() != null && !csvSymbol.name().isBlank()) ? csvSymbol.name() : normalized;
        String market = (csvSymbol.marketType() != null && !csvSymbol.marketType().isBlank())
                ? csvSymbol.marketType()
                : universeType.name();
        String exchange = (csvSymbol.exchange() != null && !csvSymbol.exchange().isBlank())
                ? csvSymbol.exchange().trim()
                : null;
        String currency = (csvSymbol.currency() != null && !csvSymbol.currency().isBlank())
                ? csvSymbol.currency().trim()
                : null;

        Symbol symbol = symbolRepository.findBySymbol(normalized)
                .orElseGet(() -> Symbol.builder()
                        .symbol(normalized)
                        .name(name)
                        .market(market)
                        .exchange(exchange)
                        .currency(currency)
                        .build());

        boolean updated = false;
        if (symbol.getName() == null || !symbol.getName().equals(name)) {
            symbol.setName(name);
            updated = true;
        }
        if (symbol.getMarket() == null || !symbol.getMarket().equals(market)) {
            symbol.setMarket(market);
            updated = true;
        }
        if (exchange != null && (symbol.getExchange() == null || !symbol.getExchange().equals(exchange))) {
            symbol.setExchange(exchange);
            updated = true;
        }
        if (currency != null && (symbol.getCurrency() == null || !symbol.getCurrency().equals(currency))) {
            symbol.setCurrency(currency);
            updated = true;
        }

        if (symbol.getId() == null || updated) {
            symbol = symbolRepository.save(symbol);
        }

        return symbol;
    }

    private Set<Symbol> loadSymbols(CatalogEntry entry, UniverseImportRequest request) {
        if ("CSV_MANUAL".equalsIgnoreCase(entry.provider())) {
            List<CsvUniverseSymbol> csvSymbols = csvUniverseLoader.loadUniverseSymbols(entry.code());
            Set<Symbol> symbols = new LinkedHashSet<>();
            for (CsvUniverseSymbol csvSymbol : csvSymbols) {
                Symbol symbol = createOrUpdateSymbolFromCsv(csvSymbol, entry.universeType());
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
            return symbols;
        }

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

        } else {
            rawSymbols = List.of();
        }

        Set<Symbol> normalized = new LinkedHashSet<>();
        for (String raw : rawSymbols) {
            String norm = normalizeSymbol(raw, entry.universeType(), request.broker());
            if (norm != null && !norm.isBlank()) {
                Symbol symbol = symbolRepository.findBySymbol(norm)
                        .orElseGet(() -> createSymbol(norm, entry.universeType()));
                normalized.add(symbol);
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

        for (CsvUniverseDefinition def : csvUniverseLoader.listDefinitions()) {
            Integer approxSize = null;
            try {
                int count = csvUniverseLoader.countSymbols(def.code());
                approxSize = count > 0 ? count : null;
            } catch (Exception e) {
                log.warn("[UniverseCatalog] Failed to count symbols for CSV universe {}", def.code(), e);
            }
            catalog.add(new CatalogEntry(
                    def.code(),
                    def.name(),
                    def.catalogType(),
                    "CSV_MANUAL",
                    approxSize,
                    true,
                    def.universeType()
            ));
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
        if (!entry.importable()) {
            return false;
        }

        String entryNameUpper = entry.name() != null ? entry.name().toUpperCase(Locale.ROOT) : "";

        return switch (requestedUpper) {
            case "SP500", "S&P500", "SPX" -> entryCodeUpper.contains("GSPC")
                    || entryCodeUpper.contains("SP500")
                    || entryNameUpper.contains("S&P 500");
            case "NASDAQ100", "NASDAQ", "NDX" -> entryCodeUpper.contains("NDX")
                    || entryCodeUpper.contains("IXIC")
                    || entryNameUpper.contains("NASDAQ");
            case "DOW", "DOWJONES", "DJI" -> entryCodeUpper.contains("DJI")
                    || entryNameUpper.contains("DOW JONES")
                    || entryNameUpper.contains("DOW");
            default -> false;
        };
    }

    private CatalogEntry buildCryptoCategoryEntry(CoingeckoUniverseClient.CoinCategory cat) {
        String code = cat.category_id();
        String name = (cat.name() != null && !cat.name().isBlank()) ? cat.name() : code;
        return new CatalogEntry(code, name, "CRYPTO_CATEGORY", "COINGECKO", null, true, UniverseType.CRYPTO);
    }

}
