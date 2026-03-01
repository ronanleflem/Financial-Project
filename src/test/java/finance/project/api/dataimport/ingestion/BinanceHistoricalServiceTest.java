package finance.project.api.dataimport.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import finance.project.api.enums.MarketType;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.BinanceService;
import finance.project.api.services.CandleAggregationService;
import finance.project.api.services.CandleService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BinanceHistoricalServiceTest {

    @Mock
    private BinanceService binanceService;

    @Mock
    private CandleService candleService;

    @Mock
    private CandleAggregationService candleAggregationService;

    @Mock
    private DeltaLakeExporter deltaLakeExporter;

    @InjectMocks
    private BinanceHistoricalService service;

    @Test
    void fetchAndSaveAggregatesAcrossMultipleTimeframesWhenSourceIs1min() {
        DataImportJob job = job("BTCUSDT", "1min");
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");
        List<CandleDTO> baseCandles = List.of(candle(LocalDateTime.parse("2024-01-01T00:00:00")));
        List<CandleDTO> aggregatedCandles = List.of(candle(LocalDateTime.parse("2024-01-01T00:01:00")));

        when(binanceService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("1min"), any(), any()))
                .thenReturn(baseCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), anyString(), eq(MarketType.CRYPTO)))
                .thenReturn(aggregatedCandles);

        boolean result = service.fetchAndSave(job, start, end);

        assertThat(result).isTrue();
        verify(candleService).saveCandlesToDatabase(baseCandles, "BTCUSDT", "1min");
        verify(deltaLakeExporter).exportCandlesToDelta(eq(job), anyList());
        verify(candleAggregationService, times(13))
                .aggregateCandles(eq(baseCandles), anyString(), eq(MarketType.CRYPTO));

        ArgumentCaptor<String> timeframeCaptor = ArgumentCaptor.forClass(String.class);
        verify(candleService, times(13)).saveCandlesToDatabase(eq(aggregatedCandles), eq("BTCUSDT"), timeframeCaptor.capture());
        assertThat(timeframeCaptor.getAllValues()).doesNotContain("1min");
    }


    @Test
    void fetchAndSaveSkipsAutoAggregationWhenSourceIsNot1min() {
        DataImportJob job = job("BTCUSDT", "1h");
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");
        List<CandleDTO> baseCandles = List.of(candle(LocalDateTime.parse("2024-01-01T00:00:00")));

        when(binanceService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("1h"), any(), any()))
                .thenReturn(baseCandles);

        boolean result = service.fetchAndSave(job, start, end);

        assertThat(result).isTrue();
        verify(candleService).saveCandlesToDatabase(baseCandles, "BTCUSDT", "1h");
        verify(deltaLakeExporter).exportCandlesToDelta(eq(job), anyList());
        verify(candleAggregationService, never()).aggregateCandles(anyList(), anyString(), any());
    }

    @Test
    void fetchAndSaveReturnsFalseWhenNoCandles() {
        DataImportJob job = job("BTCUSDT", "1h");
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");

        when(binanceService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("1h"), any(), any()))
                .thenReturn(List.of());

        boolean result = service.fetchAndSave(job, start, end);

        assertThat(result).isFalse();
        verify(candleService, never()).saveCandlesToDatabase(anyList(), anyString(), anyString());
        verify(deltaLakeExporter, never()).exportCandlesToDelta(any(), anyList());
        verify(candleAggregationService, never()).aggregateCandles(anyList(), anyString(), any());
    }

    @Test
    void fetchAndSaveReturnsFalseOnException() {
        DataImportJob job = job("BTCUSDT", "1h");
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");

        when(binanceService.getHistoricalCandlesInRange(eq("BTCUSDT"), eq("1h"), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        boolean result = service.fetchAndSave(job, start, end);

        assertThat(result).isFalse();
        verify(candleService, never()).saveCandlesToDatabase(anyList(), anyString(), anyString());
        verify(deltaLakeExporter, never()).exportCandlesToDelta(any(), anyList());
        verify(candleAggregationService, never()).aggregateCandles(anyList(), anyString(), any());
    }

    private DataImportJob job(String symbol, String timeframe) {
        DataImportJob job = new DataImportJob();
        job.setId("job-1");
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
