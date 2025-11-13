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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniverseService {

    private static final Logger log = LoggerFactory.getLogger(UniverseService.class);

    private record CatalogEntry(String code, String name, UniverseType type, String provider, int approxSize) {
    }

    private static final Map<String, CatalogEntry> CATALOG;

    static {
        Map<String, CatalogEntry> entries = new LinkedHashMap<>();
        entries.put("CRYPTO_TOP50", new CatalogEntry("CRYPTO_TOP50", "Crypto Top 50", UniverseType.CRYPTO, "COINGECKO", 50));
        entries.put("SP500", new CatalogEntry("SP500", "S&P 500", UniverseType.EQUITY, "FMB", 500));
        entries.put("NASDAQ100", new CatalogEntry("NASDAQ100", "Nasdaq 100", UniverseType.EQUITY, "FMB", 100));
        entries.put("CAC40", new CatalogEntry("CAC40", "CAC 40", UniverseType.EQUITY, "FMB", 40));
        CATALOG = Map.copyOf(entries);
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
        return CATALOG.values().stream()
                .map(entry -> new UniverseCatalogDTO(entry.code(), entry.name(), entry.type().name(), entry.provider(), entry.approxSize()))
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
        CatalogEntry entry = CATALOG.get(request.code());
        if (entry == null) {
            throw new IllegalArgumentException("Unknown universe code: " + request.code());
        }
        if (!request.startDate().isBefore(request.endDate())) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }

        Universe universe = universeRepository.findByCode(request.code())
                .orElseGet(Universe::new);

        universe.setCode(entry.code());
        universe.setName(entry.name());
        universe.setType(entry.type());
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
                    .orElseGet(() -> createSymbol(normalized, entry.type()));
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
        if (entry.type() == UniverseType.CRYPTO && "COINGECKO".equalsIgnoreCase(entry.provider())) {
            rawSymbols = coingeckoUniverseClient.fetchTopCryptoSymbols(entry.approxSize());
        } else if (entry.type() == UniverseType.EQUITY && "FMB".equalsIgnoreCase(entry.provider())) {
            rawSymbols = fmbUniverseClient.fetchIndexMembers(entry.code());
        } else {
            rawSymbols = List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : rawSymbols) {
            String norm = normalizeSymbol(raw, entry.type(), request.broker());
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
}
