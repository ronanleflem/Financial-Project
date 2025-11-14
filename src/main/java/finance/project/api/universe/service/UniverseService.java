package finance.project.api.universe.service;

import finance.project.api.entities.Symbol;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.universe.Universe;
import finance.project.api.universe.UniverseRepository;
import finance.project.api.universe.UniverseType;
import finance.project.api.universe.client.CoingeckoUniverseClient;
import finance.project.api.universe.client.FmbUniverseClient;
import finance.project.api.universe.dto.UniverseCatalogDTO;
import finance.project.api.universe.dto.UniverseDetailsDTO;
import finance.project.api.universe.dto.UniverseImportRequest;
import finance.project.api.universe.dto.UniverseImportResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
    private final FmbUniverseClient fmbUniverseClient;
    private final UniverseImportRunner universeImportRunner;

    public UniverseService(UniverseRepository universeRepository,
                           SymbolRepository symbolRepository,
                           CoingeckoUniverseClient coingeckoUniverseClient,
                           FmbUniverseClient fmbUniverseClient,
                           UniverseImportRunner universeImportRunner) {
        this.universeRepository = universeRepository;
        this.symbolRepository = symbolRepository;
        this.coingeckoUniverseClient = coingeckoUniverseClient;
        this.fmbUniverseClient = fmbUniverseClient;
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
            int limit = entry.approxSize() != null ? entry.approxSize() : 0;
            if (limit <= 0) {
                log.warn("[UniverseImport] No approxSize defined for universe {}", entry.code());
                rawSymbols = List.of();
            } else {
                rawSymbols = coingeckoUniverseClient.fetchTopCryptoSymbols(limit);
            }
        } else if (entry.universeType() == UniverseType.EQUITY
                && ("FMP".equalsIgnoreCase(entry.provider()) || "FMB".equalsIgnoreCase(entry.provider()))) {
            rawSymbols = fmbUniverseClient.fetchIndexMembers(entry.code());
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
            List<FmbUniverseClient.IndexInfo> indexes = fmbUniverseClient.listStockIndexes();
            for (FmbUniverseClient.IndexInfo idx : indexes) {
                if (idx == null || idx.symbol() == null || idx.symbol().isBlank()) {
                    continue;
                }
                catalog.add(buildEquityIndexEntry(idx));
            }
        } catch (Exception e) {
            log.warn("[UniverseCatalog] Failed to load FMP indexes list", e);
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
        return new CatalogEntry(code, name, "CRYPTO_CATEGORY", "COINGECKO", null, false, UniverseType.CRYPTO);
    }

    private CatalogEntry buildEquityIndexEntry(FmbUniverseClient.IndexInfo idx) {
        String code = idx.symbol();
        String name = (idx.name() != null && !idx.name().isBlank()) ? idx.name() : code;
        boolean importable = isImportableIndex(code, name);
        return new CatalogEntry(code, name, "EQUITY_INDEX", "FMP", null, importable, UniverseType.EQUITY);
    }

    private boolean isImportableIndex(String code, String name) {
        String upperSymbol = code != null ? code.toUpperCase(Locale.ROOT) : "";
        String upperName = name != null ? name.toUpperCase(Locale.ROOT) : "";

        if (upperSymbol.contains("GSPC") || upperSymbol.contains("SP500") || upperName.contains("S&P 500")) {
            return true;
        }
        if (upperSymbol.contains("NDX") || upperSymbol.contains("NASDAQ") || upperSymbol.contains("IXIC") || upperName.contains("NASDAQ")) {
            return true;
        }
        if (upperSymbol.contains("DJI") || upperSymbol.contains("DJIA") || upperName.contains("DOW JONES") || upperName.contains("DOW")) {
            return true;
        }
        return false;
    }
}
