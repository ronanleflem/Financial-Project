package finance.project.api.dataimport.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import finance.project.api.enums.MarketType;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.BitgetService;
import finance.project.api.services.CandleAggregationService;
import finance.project.api.services.CandleService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BitgetHistoricalServiceTest {

    @Mock
    private BitgetService bitgetService;

    @Mock
    private CandleService candleService;

    @Mock
    private CandleAggregationService candleAggregationService;

    @Mock
    private DeltaLakeExporter deltaLakeExporter;

    @InjectMocks
    private BitgetHistoricalService service;

    @Test
    void fetchAndSavePersistsAndAggregates() {
        DataImportJob job = job("BTCUSDT", "30m");
        Instant start = Instant.parse("2024-03-01T00:00:00Z");
        Instant end = Instant.parse("2024-03-01T02:00:00Z");
        List<CandleDTO> baseCandles = List.of(candle(LocalDateTime.parse("2024-03-01T00:00:00")));
        List<CandleDTO> aggregatedCandles = List.of(candle(LocalDateTime.parse("2024-03-01T00:30:00")));

        when(bitgetService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("30m"), any(), any()))
                .thenReturn(baseCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("30m"), eq(MarketType.CRYPTO)))
                .thenReturn(aggregatedCandles);

        boolean result = service.fetchAndSave(job, start, end);

        assertThat(result).isTrue();
        verify(candleService).saveCandlesToDatabase(baseCandles, "BTCUSDT", "30m");
        verify(deltaLakeExporter).exportCandlesToDelta(eq(job), anyList());
        verify(candleAggregationService).aggregateCandles(baseCandles, "30m", MarketType.CRYPTO);
        verify(candleService).saveCandlesToDatabase(aggregatedCandles, "BTCUSDT", "30m");
    }

    @Test
    void fetchAndSaveReturnsFalseWhenNoCandles() {
        DataImportJob job = job("BTCUSDT", "30m");
        Instant start = Instant.parse("2024-03-01T00:00:00Z");
        Instant end = Instant.parse("2024-03-01T02:00:00Z");

        when(bitgetService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("30m"), any(), any()))
                .thenReturn(List.of());

        boolean result = service.fetchAndSave(job, start, end);

        assertThat(result).isFalse();
        verify(candleService, never()).saveCandlesToDatabase(anyList(), any(), any());
        verify(deltaLakeExporter, never()).exportCandlesToDelta(any(), anyList());
        verify(candleAggregationService, never()).aggregateCandles(anyList(), any(), any());
    }

    @Test
    void fetchAndSaveReturnsFalseOnException() {
        DataImportJob job = job("BTCUSDT", "30m");
        Instant start = Instant.parse("2024-03-01T00:00:00Z");
        Instant end = Instant.parse("2024-03-01T02:00:00Z");

        when(bitgetService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("30m"), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        boolean result = service.fetchAndSave(job, start, end);

        assertThat(result).isFalse();
        verify(candleService, never()).saveCandlesToDatabase(anyList(), any(), any());
        verify(deltaLakeExporter, never()).exportCandlesToDelta(any(), anyList());
        verify(candleAggregationService, never()).aggregateCandles(anyList(), any(), any());
    }

    private DataImportJob job(String symbol, String timeframe) {
        DataImportJob job = new DataImportJob();
        job.setId("job-3");
        job.setSymbol(symbol);
        job.setTimeframe(timeframe);
        return job;
    }

    private CandleDTO candle(LocalDateTime date) {
        return CandleDTO.builder()
                .date(date)
                .open(BigDecimal.ONE)
                .close(BigDecimal.TEN)
                .high(BigDecimal.TEN)
                .low(BigDecimal.ONE)
                .volume(BigDecimal.ONE)
                .build();
    }
}
