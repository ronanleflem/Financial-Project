package finance.project.api.dataimport.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.infrastructure.DeltaLakeExporter;
import finance.project.api.enums.MarketType;
import finance.project.api.model.CandleDTO;
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
class DatabentoCsvImportServiceTest {

    @Mock
    private CandleService candleService;

    @Mock
    private CandleAggregationService candleAggregationService;

    @Mock
    private DeltaLakeExporter deltaLakeExporter;

    @InjectMocks
    private DatabentoCsvImportService service;

    @Test
    void importCsvAggregatesAcrossTimeframes() {
        DataImportJob job = job("ES", "1min");
        Instant start = Instant.parse("2024-02-01T00:00:00Z");
        Instant end = Instant.parse("2024-02-02T00:00:00Z");
        List<CandleDTO> baseCandles = List.of(candle(LocalDateTime.parse("2024-02-01T00:00:00")));
        List<CandleDTO> aggregatedCandles = List.of(candle(LocalDateTime.parse("2024-02-01T00:01:00")));

        when(candleService.loadCsvCME("ES", "1min", "data_2024-02")).thenReturn(baseCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("3min"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("5min"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("10min"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("15min"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("30min"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("1h"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("2h"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("4h"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("8h"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("12h"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("daily"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("weekly"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);
        when(candleAggregationService.aggregateCandles(eq(baseCandles), eq("monthly"), eq(MarketType.CME)))
                .thenReturn(aggregatedCandles);

        service.importCsv(job, start, end, null);

        verify(candleService).saveCandlesToDatabase(baseCandles, "ES", "1min");
        verify(deltaLakeExporter).exportCandlesToDelta(eq(job), anyList());
        verify(candleAggregationService, times(13)).aggregateCandles(eq(baseCandles), any(), eq(MarketType.CME));

        ArgumentCaptor<String> timeframeCaptor = ArgumentCaptor.forClass(String.class);
        verify(candleService, times(13)).saveCandlesToDatabase(eq(aggregatedCandles), eq("ES"), timeframeCaptor.capture());
        assertThat(timeframeCaptor.getAllValues()).contains("monthly");
    }

    @Test
    void importCsvSkipsWhenNoCandles() {
        DataImportJob job = job("ES", "1min");
        Instant start = Instant.parse("2024-02-01T00:00:00Z");
        Instant end = Instant.parse("2024-02-02T00:00:00Z");

        when(candleService.loadCsvCME("ES", "1min", "data_2024-02")).thenReturn(List.of());

        service.importCsv(job, start, end, null);

        verify(candleService, never()).saveCandlesToDatabase(anyList(), eq("ES"), eq("1min"));
        verify(deltaLakeExporter, never()).exportCandlesToDelta(eq(job), anyList());
        verify(candleAggregationService, never()).aggregateCandles(anyList(), any(), any());
    }

    @Test
    void importCsvStopsWhenLoadThrows() {
        DataImportJob job = job("ES", "1min");
        Instant start = Instant.parse("2024-02-01T00:00:00Z");
        Instant end = Instant.parse("2024-02-02T00:00:00Z");

        when(candleService.loadCsvCME("ES", "1min", "data_2024-02"))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.importCsv(job, start, end, null))
                .isInstanceOf(RuntimeException.class);

        verify(candleService, never()).saveCandlesToDatabase(anyList(), eq("ES"), eq("1min"));
        verify(deltaLakeExporter, never()).exportCandlesToDelta(eq(job), anyList());
        verify(candleAggregationService, never()).aggregateCandles(anyList(), any(), any());
    }

    private DataImportJob job(String symbol, String timeframe) {
        DataImportJob job = new DataImportJob();
        job.setId("job-4");
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
