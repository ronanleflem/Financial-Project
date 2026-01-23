package finance.project.api.dataimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import finance.project.api.dataimport.ingestion.BinanceHistoricalService;
import finance.project.api.dataimport.ingestion.BybitHistoricalService;
import finance.project.api.dataimport.ingestion.CsvImportService;
import finance.project.api.dataimport.ingestion.DatabentoCsvImportService;
import finance.project.api.dataimport.ingestion.DukascopyHistoricalService;
import finance.project.api.dataimport.ingestion.IbkrImportService;
import finance.project.api.dataimport.ingestion.MexcHistoricalService;
import finance.project.api.dataimport.ingestion.OkxHistoricalService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataImportJobRunnerTest {

    @Mock
    private DataImportJobRepository jobRepository;

    @Mock
    private BinanceHistoricalService binanceHistoricalService;

    @Mock
    private OkxHistoricalService okxHistoricalService;

    @Mock
    private BybitHistoricalService bybitHistoricalService;

    @Mock
    private MexcHistoricalService mexcHistoricalService;

    @Mock
    private IbkrImportService ibkrImportService;

    @Mock
    private DatabentoCsvImportService databentoCsvImportService;

    @Mock
    private CsvImportService csvImportService;

    @Mock
    private DukascopyHistoricalService dukascopyHistoricalService;

    @InjectMocks
    private DataImportJobRunner runner;

    private DataImportJob job;

    @BeforeEach
    void setUp() {
        job = new DataImportJob();
        job.setId("job-123");
        job.setBroker("BINANCE");
        job.setSymbol("BTCUSDT");
        job.setTimeframe("1h");
        job.setStartDate(Instant.parse("2024-01-01T00:00:00Z"));
        job.setEndDate(Instant.parse("2024-01-02T00:00:00Z"));
        job.setSourceType("API");
        job.setStatus(DataImportJob.Status.PENDING);
        job.setProgress(0);
    }

    @Test
    void runJobAsyncUpdatesJobToSuccess() {
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(DataImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(binanceHistoricalService.fetchAndSave(any(), any(), any()))
                .thenReturn(true);

        runner.runJobAsync("job-123");

        assertThat(job.getStatus()).isEqualTo(DataImportJob.Status.SUCCESS);
        assertThat(job.getProgress()).isEqualTo(100);
        assertThat(job.getMessage()).isEqualTo("Import terminé");
        verify(binanceHistoricalService).fetchAndSave(any(), any(), any());
    }

    @Test
    void runJobAsyncCapturesFailure() {
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(DataImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("network down"))
                .when(binanceHistoricalService)
                .fetchAndSave(any(), any(), any());

        runner.runJobAsync("job-123");

        assertThat(job.getStatus()).isEqualTo(DataImportJob.Status.FAILED);
        assertThat(job.getMessage()).contains("Erreur :");
        assertThat(job.getProgress()).isLessThanOrEqualTo(99);
    }

    @Test
    void csvJobDelegatesToDatabentoOnce() {
        job.setBroker("DATABENTO_CSV");
        job.setSourceType("CSV");
        job.setVenue("data_2024-03");
        job.setTimeframe("1min");
        job.setStartDate(Instant.parse("2024-03-01T00:00:00Z"));
        job.setEndDate(Instant.parse("2024-03-31T23:59:59Z"));

        when(jobRepository.findById("job-123")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(DataImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        runner.runJobAsync("job-123");

        verify(databentoCsvImportService)
                .importCsv(any(), any(), any(), any());
    }
}
