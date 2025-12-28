package finance.project.api.universe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import finance.project.api.entities.Symbol;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.universe.Universe;
import finance.project.api.universe.UniverseRepository;
import finance.project.api.universe.UniverseType;
import finance.project.api.universe.client.CoingeckoUniverseClient;
import finance.project.api.universe.client.CoingeckoUniverseClient.CoinCategory;
import finance.project.api.universe.csv.CsvUniverseLoader;
import finance.project.api.universe.csv.CsvUniverseLoader.CsvUniverseDefinition;
import finance.project.api.universe.csv.CsvUniverseLoader.CsvUniverseSymbol;
import finance.project.api.universe.dto.UniverseImportRequest;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UniverseServiceTest {

    @Mock
    private UniverseRepository universeRepository;

    @Mock
    private SymbolRepository symbolRepository;

    @Mock
    private CoingeckoUniverseClient coingeckoUniverseClient;

    @Mock
    private CsvUniverseLoader csvUniverseLoader;

    @Mock
    private UniverseImportRunner universeImportRunner;

    private UniverseService universeService;

    @BeforeEach
    void setUp() {
        universeService = new UniverseService(
                universeRepository,
                symbolRepository,
                coingeckoUniverseClient,
                csvUniverseLoader,
                universeImportRunner
        );
    }

    @Test
    void getCatalog_includesCoingeckoAndCsvEntries() {
        when(coingeckoUniverseClient.listCategories())
                .thenReturn(List.of(new CoinCategory("ai-agents", "AI Agents")));
        when(csvUniverseLoader.listDefinitions()).thenReturn(List.of(
                new CsvUniverseDefinition("SP500", "S&P 500", "EQUITY_INDEX_CSV", UniverseType.EQUITY, "sp500.csv")
        ));
        when(csvUniverseLoader.countSymbols("SP500")).thenReturn(3);

        var catalog = universeService.getCatalog();

        assertThat(catalog).hasSize(2);
        assertThat(catalog)
                .anyMatch(entry -> "ai-agents".equals(entry.code()) && "COINGECKO".equals(entry.provider()))
                .anyMatch(entry -> "SP500".equals(entry.code()) && entry.approxSize() == 3);
    }

    @Test
    void getCatalog_whenCoingeckoFails_returnsCsvEntries() {
        when(coingeckoUniverseClient.listCategories()).thenThrow(new IllegalStateException("boom"));
        when(csvUniverseLoader.listDefinitions()).thenReturn(List.of(
                new CsvUniverseDefinition("CAC40", "CAC 40", "EQUITY_INDEX_CSV", UniverseType.EQUITY, "cac40.csv")
        ));
        when(csvUniverseLoader.countSymbols("CAC40")).thenReturn(12);

        var catalog = universeService.getCatalog();

        assertThat(catalog).hasSize(1);
        assertThat(catalog.getFirst().code()).isEqualTo("CAC40");
        assertThat(catalog.getFirst().approxSize()).isEqualTo(12);
    }

    @Test
    void importUniverse_rejectsUnknownCode() {
        TestUniverseService testService = new TestUniverseService(List.of());
        UniverseImportRequest request = new UniverseImportRequest(
                "UNKNOWN",
                "BINANCE",
                "1h",
                Instant.parse("2023-01-02T00:00:00Z"),
                Instant.parse("2023-01-03T00:00:00Z"),
                null,
                null
        );

        assertThatThrownBy(() -> testService.importUniverse(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown universe code: UNKNOWN");
    }

    @Test
    void importUniverse_rejectsNonImportableUniverse() {
        TestUniverseService testService = new TestUniverseService(List.of(
                new UniverseService.CatalogEntry(
                        "PRIVATE",
                        "Private Universe",
                        "CSV_PRIVATE",
                        "CSV_MANUAL",
                        null,
                        false,
                        UniverseType.EQUITY
                )
        ));
        UniverseImportRequest request = new UniverseImportRequest(
                "PRIVATE",
                "BINANCE",
                "1h",
                Instant.parse("2023-01-02T00:00:00Z"),
                Instant.parse("2023-01-03T00:00:00Z"),
                null,
                null
        );

        assertThatThrownBy(() -> testService.importUniverse(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Universe is not importable: PRIVATE");
    }

    @Test
    void importUniverse_rejectsInvalidDates() {
        TestUniverseService testService = new TestUniverseService(List.of(
                new UniverseService.CatalogEntry(
                        "SP500",
                        "S&P 500",
                        "EQUITY_INDEX_CSV",
                        "CSV_MANUAL",
                        null,
                        true,
                        UniverseType.EQUITY
                )
        ));
        UniverseImportRequest request = new UniverseImportRequest(
                "SP500",
                "IBKR",
                "1d",
                Instant.parse("2023-01-03T00:00:00Z"),
                Instant.parse("2023-01-03T00:00:00Z"),
                null,
                null
        );

        assertThatThrownBy(() -> testService.importUniverse(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startDate must be before endDate");
    }

    @Test
    void normalizeSymbol_handlesCryptoFormatting() {
        String normalized = ReflectionTestUtils.invokeMethod(
                universeService,
                "normalizeSymbol",
                "btc",
                UniverseType.CRYPTO,
                "BINANCE"
        );

        assertThat(normalized).isEqualTo("BTCUSDT");
    }

    @Test
    void importUniverse_fromCsv_createsAndUpdatesSymbols() {
        TestUniverseService testService = new TestUniverseService(List.of(
                new UniverseService.CatalogEntry(
                        "CSV_UNIVERSE",
                        "CSV Universe",
                        "EQUITY_INDEX_CSV",
                        "CSV_MANUAL",
                        2,
                        true,
                        UniverseType.EQUITY
                )
        ));
        List<CsvUniverseSymbol> csvSymbols = List.of(
                new CsvUniverseSymbol("AAPL", "Apple Inc", "NASDAQ", "USD", "EQUITY", "IBKR", "EQUITY", ""),
                new CsvUniverseSymbol("msft", "Microsoft", "", "", "", "", "", "")
        );
        when(csvUniverseLoader.loadUniverseSymbols("CSV_UNIVERSE")).thenReturn(csvSymbols);

        Symbol existing = Symbol.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .name("Old Name")
                .market("OLD")
                .exchange("OLD")
                .currency("EUR")
                .build();
        when(symbolRepository.findBySymbol("AAPL")).thenReturn(Optional.of(existing));
        when(symbolRepository.findBySymbol("MSFT")).thenReturn(Optional.empty());
        when(symbolRepository.save(any(Symbol.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(universeRepository.findByCode("CSV_UNIVERSE")).thenReturn(Optional.empty());
        when(universeRepository.save(any(Universe.class))).thenAnswer(invocation -> {
            Universe universe = invocation.getArgument(0);
            if (universe.getId() == null) {
                universe.setId(42L);
            }
            return universe;
        });

        UniverseImportRequest request = new UniverseImportRequest(
                "CSV_UNIVERSE",
                "IBKR",
                "1d",
                Instant.parse("2023-01-01T00:00:00Z"),
                Instant.parse("2023-01-02T00:00:00Z"),
                null,
                null
        );

        testService.importUniverse(request);

        ArgumentCaptor<Symbol> symbolCaptor = ArgumentCaptor.forClass(Symbol.class);
        verify(symbolRepository, org.mockito.Mockito.times(2)).save(symbolCaptor.capture());
        List<Symbol> savedSymbols = symbolCaptor.getAllValues();
        assertThat(savedSymbols)
                .anyMatch(symbol -> "AAPL".equals(symbol.getSymbol())
                        && "Apple Inc".equals(symbol.getName())
                        && "EQUITY".equals(symbol.getMarket())
                        && "NASDAQ".equals(symbol.getExchange())
                        && "USD".equals(symbol.getCurrency()))
                .anyMatch(symbol -> "MSFT".equals(symbol.getSymbol())
                        && "Microsoft".equals(symbol.getName())
                        && "EQUITY".equals(symbol.getMarket()));

        ArgumentCaptor<Universe> universeCaptor = ArgumentCaptor.forClass(Universe.class);
        verify(universeRepository).save(universeCaptor.capture());
        Universe savedUniverse = universeCaptor.getValue();
        assertThat(savedUniverse.getSymbols()).hasSize(2);
        verify(universeImportRunner).runUniverseImport(eq(42L), eq(request));
    }

    @Test
    void importUniverse_fromCoingecko_addsUniverseSymbolAssociations() {
        TestUniverseService testService = new TestUniverseService(List.of(
                new UniverseService.CatalogEntry(
                        "TOP_CRYPTO",
                        "Top Crypto",
                        "CRYPTO_TOP",
                        "COINGECKO",
                        2,
                        true,
                        UniverseType.CRYPTO
                )
        ));
        when(coingeckoUniverseClient.fetchTopCryptoSymbols(2)).thenReturn(List.of("eth/usd", "btc"));

        Symbol eth = Symbol.builder()
                .symbol("ETHUSD")
                .name("ETHUSD")
                .market("CRYPTO")
                .universes(new LinkedHashSet<>())
                .build();
        Symbol btc = Symbol.builder()
                .symbol("BTCUSDT")
                .name("BTCUSDT")
                .market("CRYPTO")
                .universes(new LinkedHashSet<>())
                .build();
        when(symbolRepository.findBySymbol("ETHUSD")).thenReturn(Optional.of(eth));
        when(symbolRepository.findBySymbol("BTCUSDT")).thenReturn(Optional.of(btc));
        when(universeRepository.findByCode("TOP_CRYPTO")).thenReturn(Optional.empty());
        when(universeRepository.save(any(Universe.class))).thenAnswer(invocation -> {
            Universe universe = invocation.getArgument(0);
            if (universe.getId() == null) {
                universe.setId(9L);
            }
            return universe;
        });

        UniverseImportRequest request = new UniverseImportRequest(
                "TOP_CRYPTO",
                "BINANCE",
                "1h",
                Instant.parse("2023-02-01T00:00:00Z"),
                Instant.parse("2023-02-02T00:00:00Z"),
                null,
                null
        );

        testService.importUniverse(request);

        verify(symbolRepository).findBySymbol("ETHUSD");
        verify(symbolRepository).findBySymbol("BTCUSDT");

        ArgumentCaptor<Universe> universeCaptor = ArgumentCaptor.forClass(Universe.class);
        verify(universeRepository).save(universeCaptor.capture());
        Universe savedUniverse = universeCaptor.getValue();
        assertThat(savedUniverse.getSymbols()).hasSize(2);
        for (Symbol symbol : savedUniverse.getSymbols()) {
            assertThat(symbol.getUniverses()).contains(savedUniverse);
        }
        verify(universeImportRunner).runUniverseImport(eq(9L), eq(request));
    }

    @Test
    void findCatalogEntry_resolvesAliases() {
        TestUniverseService testService = new TestUniverseService(List.of(
                new UniverseService.CatalogEntry(
                        "GSPC",
                        "S&P 500",
                        "EQUITY_INDEX_CSV",
                        "CSV_MANUAL",
                        null,
                        true,
                        UniverseType.EQUITY
                ),
                new UniverseService.CatalogEntry(
                        "IXIC",
                        "Nasdaq Composite",
                        "EQUITY_INDEX_CSV",
                        "CSV_MANUAL",
                        null,
                        true,
                        UniverseType.EQUITY
                ),
                new UniverseService.CatalogEntry(
                        "DJI",
                        "Dow Jones Industrial Average",
                        "EQUITY_INDEX_CSV",
                        "CSV_MANUAL",
                        null,
                        true,
                        UniverseType.EQUITY
                )
        ));

        Optional<?> sp500 = ReflectionTestUtils.invokeMethod(testService, "findCatalogEntry", "SP500");
        Optional<?> nasdaq = ReflectionTestUtils.invokeMethod(testService, "findCatalogEntry", "NASDAQ");
        Optional<?> dow = ReflectionTestUtils.invokeMethod(testService, "findCatalogEntry", "DOW");

        assertThat(sp500).isPresent();
        assertThat(nasdaq).isPresent();
        assertThat(dow).isPresent();
        assertThat(sp500.map(entry -> ((UniverseService.CatalogEntry) entry).code()).orElse(""))
                .isEqualTo("GSPC");
        assertThat(nasdaq.map(entry -> ((UniverseService.CatalogEntry) entry).code()).orElse(""))
                .isEqualTo("IXIC");
        assertThat(dow.map(entry -> ((UniverseService.CatalogEntry) entry).code()).orElse(""))
                .isEqualTo("DJI");
    }

    private class TestUniverseService extends UniverseService {
        private List<CatalogEntry> catalogEntries;

        private TestUniverseService(List<CatalogEntry> catalogEntries) {
            super(universeRepository, symbolRepository, coingeckoUniverseClient, csvUniverseLoader, universeImportRunner);
            this.catalogEntries = catalogEntries;
        }

        @Override
        protected List<CatalogEntry> buildCatalogEntries() {
            return catalogEntries;
        }
    }
}
