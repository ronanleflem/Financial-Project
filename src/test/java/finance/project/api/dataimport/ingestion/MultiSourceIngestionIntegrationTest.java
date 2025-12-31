package finance.project.api.dataimport.ingestion;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.model.CandleDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.BinanceService;
import finance.project.api.services.CandleService;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class MultiSourceIngestionIntegrationTest {

    @Autowired
    private BinanceHistoricalService binanceHistoricalService;

    @Autowired
    private CandleService candleService;

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @MockBean
    private BinanceService binanceService;

    @MockBean
    private DeltaLakeExporter deltaLakeExporter;

    @BeforeEach
    void setUp() {
        symbolRepository.save(Symbol.builder()
                .symbol("BTCUSDT")
                .name("Bitcoin")
                .market("CRYPTO")
                .exchange("BINANCE")
                .currency("USDT")
                .build());
        symbolRepository.save(Symbol.builder()
                .symbol("EURUSD")
                .name("EURUSD")
                .market("FX")
                .exchange("CSV")
                .currency("USD")
                .build());
    }

    @AfterEach
    void tearDown() {
        candleRepository.deleteAll();
        symbolRepository.deleteAll();
    }

    @Test
    void multiSourceIngestionPersistsBaseAndAggregatedCandles() {
        DataImportJob job = new DataImportJob();
        job.setSymbol("BTCUSDT");
        job.setTimeframe("1min");

        LocalDateTime baseTime = LocalDateTime.parse("2024-01-01T00:00:00");
        List<CandleDTO> candles = buildCandles(baseTime, 60);

        when(binanceService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("1min"), any(), any()))
                .thenReturn(candles);

        boolean result = binanceHistoricalService.fetchAndSave(
                job,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T01:00:00Z")
        );

        assertThat(result).isTrue();

        Symbol btc = symbolRepository.findBySymbol("BTCUSDT").orElseThrow();
        assertThat(candleRepository.findBySymbolAndTimeframeOrderByDateAsc(btc, "1min"))
                .hasSize(60);
        assertThat(candleRepository.findBySymbolAndTimeframeOrderByDateAsc(btc, "3min"))
                .hasSize(20);
        assertThat(candleRepository.findBySymbolAndTimeframeOrderByDateAsc(btc, "5min"))
                .hasSize(12);
        assertThat(candleRepository.findBySymbolAndTimeframeOrderByDateAsc(btc, "1h"))
                .hasSize(1);

        Candle aggregated3m = candleRepository.findBySymbolAndTimeframeOrderByDateAsc(btc, "3min").get(0);
        assertThat(aggregated3m.getDate()).isEqualTo(baseTime);
        assertThat(aggregated3m.getOpen()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(aggregated3m.getClose()).isEqualByComparingTo(new BigDecimal("103"));
        assertThat(aggregated3m.getHigh()).isEqualByComparingTo(new BigDecimal("104"));
        assertThat(aggregated3m.getLow()).isEqualByComparingTo(new BigDecimal("99"));
        assertThat(aggregated3m.getVolume()).isEqualByComparingTo(new BigDecimal("33"));

        Candle aggregated5m = candleRepository.findBySymbolAndTimeframeOrderByDateAsc(btc, "5min").get(0);
        assertThat(aggregated5m.getDate()).isEqualTo(baseTime);
        assertThat(aggregated5m.getOpen()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(aggregated5m.getClose()).isEqualByComparingTo(new BigDecimal("105"));
        assertThat(aggregated5m.getHigh()).isEqualByComparingTo(new BigDecimal("106"));
        assertThat(aggregated5m.getLow()).isEqualByComparingTo(new BigDecimal("99"));
        assertThat(aggregated5m.getVolume()).isEqualByComparingTo(new BigDecimal("60"));

        Candle aggregated1h = candleRepository.findBySymbolAndTimeframeOrderByDateAsc(btc, "1h").get(0);
        assertThat(aggregated1h.getDate()).isEqualTo(baseTime);
        assertThat(aggregated1h.getOpen()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(aggregated1h.getClose()).isEqualByComparingTo(new BigDecimal("160"));
        assertThat(aggregated1h.getHigh()).isEqualByComparingTo(new BigDecimal("161"));
        assertThat(aggregated1h.getLow()).isEqualByComparingTo(new BigDecimal("99"));
        assertThat(aggregated1h.getVolume()).isEqualByComparingTo(new BigDecimal("2370"));

        List<CandleDTO> csvCandles = candleService.loadCsvTradingView("EURUSD", "3min", false);
        assertThat(csvCandles).isNotEmpty();

        Symbol eurusd = symbolRepository.findBySymbol("EURUSD").orElseThrow();
        List<Candle> persistedCsv = candleRepository.findBySymbolAndTimeframeOrderByDateAsc(eurusd, "3min");
        assertThat(persistedCsv).isNotEmpty();
    }

    @Test
    void noDataDoesNotPersistCandles() {
        DataImportJob job = new DataImportJob();
        job.setSymbol("BTCUSDT");
        job.setTimeframe("1min");

        when(binanceService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("1min"), any(), any()))
                .thenReturn(List.of());

        boolean result = binanceHistoricalService.fetchAndSave(
                job,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T01:00:00Z")
        );

        assertThat(result).isFalse();

        Symbol btc = symbolRepository.findBySymbol("BTCUSDT").orElseThrow();
        assertThat(candleRepository.findBySymbolAndTimeframeOrderByDateAsc(btc, "1min")).isEmpty();
    }

    private List<CandleDTO> buildCandles(LocalDateTime start, int count) {
        List<CandleDTO> candles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BigDecimal open = BigDecimal.valueOf(100L + i);
            BigDecimal high = open.add(BigDecimal.valueOf(2));
            BigDecimal low = open.subtract(BigDecimal.ONE);
            BigDecimal close = open.add(BigDecimal.ONE);
            BigDecimal volume = BigDecimal.valueOf(10L + i);
            candles.add(CandleDTO.builder()
                    .date(start.plusMinutes(i))
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .volume(volume)
                    .build());
        }
        return candles;
    }
}
