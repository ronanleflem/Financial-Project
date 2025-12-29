package finance.project.api.dataimport.ingestion;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.model.CandleDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.BinanceService;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
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
class BinanceHistoricalServiceIntegrationTest {

    @Autowired
    private BinanceHistoricalService binanceHistoricalService;

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
        Symbol symbol = Symbol.builder()
                .symbol("BTCUSDT")
                .name("Bitcoin")
                .market("CRYPTO")
                .exchange("BINANCE")
                .currency("USDT")
                .build();
        symbolRepository.save(symbol);
    }

    @AfterEach
    void tearDown() {
        candleRepository.deleteAll();
        symbolRepository.deleteAll();
    }

    @Test
    void fetchAndSavePersistsAndAggregatesCandles() {
        DataImportJob job = new DataImportJob();
        job.setSymbol("BTCUSDT");
        job.setTimeframe("1min");

        LocalDateTime baseTime = LocalDateTime.parse("2024-01-01T00:00:00");
        List<CandleDTO> candles = List.of(
                candleAt(baseTime, "100", "101", "102", "99", "10"),
                candleAt(baseTime.plusMinutes(1), "101", "102", "103", "100", "11"),
                candleAt(baseTime.plusMinutes(2), "102", "103", "104", "101", "12"),
                candleAt(baseTime.plusMinutes(3), "103", "104", "105", "102", "13"),
                candleAt(baseTime.plusMinutes(4), "104", "105", "106", "103", "14")
        );

        when(binanceService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("1min"), any(), any()))
                .thenReturn(candles);

        boolean result = binanceHistoricalService.fetchAndSave(
                job,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:05:00Z")
        );

        assertThat(result).isTrue();

        Symbol symbol = symbolRepository.findBySymbol("BTCUSDT").orElseThrow();
        List<Candle> baseCandles = candleRepository.findBySymbolAndTimeframeOrderByDateAsc(symbol, "1min");
        assertThat(baseCandles).hasSize(5);

        List<Candle> aggregatedCandles = candleRepository.findBySymbolAndTimeframeOrderByDateAsc(symbol, "5min");
        assertThat(aggregatedCandles).hasSize(1);

        Candle aggregated = aggregatedCandles.get(0);
        assertThat(aggregated.getDate()).isEqualTo(baseTime);
        assertThat(aggregated.getOpen()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(aggregated.getClose()).isEqualByComparingTo(new BigDecimal("105"));
        assertThat(aggregated.getHigh()).isEqualByComparingTo(new BigDecimal("106"));
        assertThat(aggregated.getLow()).isEqualByComparingTo(new BigDecimal("99"));
        assertThat(aggregated.getVolume()).isEqualByComparingTo(new BigDecimal("60"));
    }

    private CandleDTO candleAt(LocalDateTime time, String open, String close, String high, String low, String volume) {
        return CandleDTO.builder()
                .date(time)
                .open(new BigDecimal(open))
                .close(new BigDecimal(close))
                .high(new BigDecimal(high))
                .low(new BigDecimal(low))
                .volume(new BigDecimal(volume))
                .build();
    }
}
