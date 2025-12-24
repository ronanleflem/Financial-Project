package finance.project.api.universe.csv;

import finance.project.api.universe.UniverseType;
import finance.project.api.universe.dto.UniverseCatalogDTO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class CsvUniverseLoader {

    private static final Logger log = LoggerFactory.getLogger(CsvUniverseLoader.class);

    private static final Map<String, CsvUniverseDefinition> DEFINITIONS;

    static {
        Map<String, CsvUniverseDefinition> defs = new LinkedHashMap<>();
        defs.put("CRYPTO_TOP100_CAP", new CsvUniverseDefinition(
                "CRYPTO_TOP100_CAP",
                "Top 100 Crypto Cap (CSV)",
                "CRYPTO_UNIVERSE_CSV",
                UniverseType.CRYPTO,
                "crypto_top100_cap.csv"
        ));
        defs.put("CRYPTO_SPOT_PERSONAL", new CsvUniverseDefinition(
                "CRYPTO_SPOT_PERSONAL",
                "Portefeuille Crypto Perso (CSV)",
                "CRYPTO_UNIVERSE_CSV",
                UniverseType.CRYPTO,
                "crypto_spot_personal.csv"
        ));
        defs.put("SP500", new CsvUniverseDefinition(
                "SP500",
                "S&P 500 (CSV)",
                "EQUITY_INDEX_CSV",
                UniverseType.EQUITY,
                "sp500.csv"
        ));
        defs.put("NASDAQ100", new CsvUniverseDefinition(
                "NASDAQ100",
                "Nasdaq 100 (CSV)",
                "EQUITY_INDEX_CSV",
                UniverseType.EQUITY,
                "nasdaq100.csv"
        ));
        defs.put("AAPLMSFT", new CsvUniverseDefinition(
                "AAPLMSFT",
                "AAPLMSFT (CSV)",
                "EQUITY_INDEX_CSV",
                UniverseType.EQUITY,
                "aaplmsft.csv"
        ));
        defs.put("DOWJONES", new CsvUniverseDefinition(
                "DOWJONES",
                "Dow Jones (CSV)",
                "EQUITY_INDEX_CSV",
                UniverseType.EQUITY,
                "dowjones.csv"
        ));
        defs.put("CAC40", new CsvUniverseDefinition(
                "CAC40",
                "CAC 40 (CSV)",
                "EQUITY_INDEX_CSV",
                UniverseType.EQUITY,
                "cac40.csv"
        ));
        defs.put("STOXX600", new CsvUniverseDefinition(
                "STOXX600",
                "STOXX 600 (CSV)",
                "EQUITY_INDEX_CSV",
                UniverseType.EQUITY,
                "stoxx600.csv"
        ));
        defs.put("ETF_UNIVERSE", new CsvUniverseDefinition(
                "ETF_UNIVERSE",
                "ETF Universe (CSV)",
                "ETF_UNIVERSE_CSV",
                UniverseType.ETF,
                "universe_etf.csv"
        ));
        DEFINITIONS = Map.copyOf(defs);
    }

    public List<UniverseCatalogDTO> listCsvUniverses() {
        List<UniverseCatalogDTO> catalog = new ArrayList<>();
        for (CsvUniverseDefinition def : DEFINITIONS.values()) {
            int approx = countSymbols(def.code());
            catalog.add(new UniverseCatalogDTO(
                    def.code(),
                    def.name(),
                    def.catalogType(),
                    "CSV_MANUAL",
                    approx > 0 ? approx : null,
                    true
            ));
        }
        return catalog;
    }

    public List<CsvUniverseDefinition> listDefinitions() {
        return new ArrayList<>(DEFINITIONS.values());
    }

    public Optional<CsvUniverseDefinition> findDefinition(String universeCode) {
        if (universeCode == null || universeCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = universeCode.trim().toUpperCase(Locale.ROOT);
        return DEFINITIONS.values().stream()
                .filter(def -> def.code().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public List<CsvUniverseSymbol> loadUniverseSymbols(String universeCode) {
        CsvUniverseDefinition def = findDefinition(universeCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown CSV universe: " + universeCode));
        return readSymbols(def.fileName());
    }

    public int countSymbols(String universeCode) {
        try {
            return loadUniverseSymbols(universeCode).size();
        } catch (Exception e) {
            log.warn("[CSV Universe] Failed to count symbols for {}", universeCode, e);
            return 0;
        }
    }

    private List<CsvUniverseSymbol> readSymbols(String fileName) {
        Resource resource = new ClassPathResource("univers/csv/" + fileName);
        if (!resource.exists()) {
            log.warn("[CSV Universe] Resource not found: {}", fileName);
            return List.of();
        }

        List<CsvUniverseSymbol> symbols = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerParsed = false;
            Map<String, Integer> headerIndex = new LinkedHashMap<>();
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split(",", -1);
                if (!headerParsed) {
                    headerParsed = true;
                    for (int i = 0; i < parts.length; i++) {
                        String header = parts[i].trim().toLowerCase(Locale.ROOT);
                        if (!header.isEmpty()) {
                            headerIndex.put(header, i);
                        }
                    }
                    continue;
                }
                String symbol = safeColumn(parts, headerIndex, "symbol");
                String isin = safeColumn(parts, headerIndex, "isin");
                String name = safeColumn(parts, headerIndex, "name");
                String label = safeColumn(parts, headerIndex, "label");
                if (name.isBlank() && !label.isBlank()) {
                    name = label;
                }
                String exchange = safeColumn(parts, headerIndex, "exchange");
                String currency = safeColumn(parts, headerIndex, "currency");
                String marketType = safeColumn(parts, headerIndex, "markettype");
                String assetClass = safeColumn(parts, headerIndex, "assetclass");
                String defaultBroker = safeColumn(parts, headerIndex, "defaultbroker");
                if (defaultBroker.isBlank()) {
                    defaultBroker = safeColumn(parts, headerIndex, "broker");
                }
                if (symbol.isBlank() && isin.isBlank()) {
                    continue;
                }
                symbols.add(new CsvUniverseSymbol(
                        symbol,
                        name,
                        exchange,
                        currency,
                        marketType,
                        defaultBroker,
                        assetClass,
                        isin
                ));
            }
        } catch (IOException e) {
            log.warn("[CSV Universe] Failed to read {}", fileName, e);
        }
        return symbols;
    }

    private String safeColumn(String[] parts, Map<String, Integer> headerIndex, String column) {
        Integer idx = headerIndex.get(column);
        if (idx == null) {
            return "";
        }
        return safePart(parts, idx);
    }

    private String safePart(String[] parts, int idx) {
        return parts.length > idx ? parts[idx].trim() : "";
    }

    public record CsvUniverseSymbol(
            String symbol,
            String name,
            String exchange,
            String currency,
            String marketType,
            String defaultBroker,
            String assetClass,
            String isin
    ) {
    }

    public record CsvUniverseDefinition(
            String code,
            String name,
            String catalogType,
            UniverseType universeType,
            String fileName
    ) {
    }
}
