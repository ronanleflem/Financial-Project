package finance.project.api.dataimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import finance.project.api.dataimport.dto.DataImportRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataImportServiceTest {

    @Mock
    private DataImportJobRepository jobRepository;

    @Mock
    private DataImportJobRunner jobRunner;

    @InjectMocks
    private DataImportService service;

    @Test
    void createJobPersistsAndTriggersRunner() {
        DataImportRequest request = new DataImportRequest(
                "BINANCE",
                "BTCUSDT",
                "1h",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"),
                "API",
                null,
                null,
                null,
                null
        );

        when(jobRepository.save(any(DataImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(jobRunner).runJobAsync(any());

        DataImportJob job = service.createJob(request);

        ArgumentCaptor<DataImportJob> captor = ArgumentCaptor.forClass(DataImportJob.class);
        verify(jobRepository).save(captor.capture());
        DataImportJob persisted = captor.getValue();

        assertThat(job.getId()).isNotBlank();
        assertThat(persisted.getStatus()).isEqualTo(DataImportJob.Status.PENDING);
        assertThat(persisted.getProgress()).isZero();
        verify(jobRunner).runJobAsync(eq(job.getId()));
    }

    @Test
    void createJobCopiesOptionalMetadata() {
        DataImportRequest request = new DataImportRequest(
                "DATABENTO_CSV",
                "EURUSD",
                "1min",
                Instant.parse("2024-03-01T00:00:00Z"),
                Instant.parse("2024-03-31T23:59:59Z"),
                "CSV",
                "data_2024-03",
                "UTC",
                "merge",
                "volume"
        );

        when(jobRepository.save(any(DataImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataImportJob job = service.createJob(request);

        assertThat(job.getVenue()).isEqualTo("data_2024-03");
        assertThat(job.getTimezone()).isEqualTo("UTC");
        assertThat(job.getConflictPolicy()).isEqualTo("merge");
        assertThat(job.getRollover()).isEqualTo("volume");
    }

    @Test
    void createJobWithInvalidRangeThrows() {
        DataImportRequest request = new DataImportRequest(
                "BINANCE",
                "BTCUSDT",
                "1h",
                Instant.parse("2024-01-02T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"),
                "API",
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate must be before endDate");
    }

    @Test
    void getJobDelegatesToRepository() {
        DataImportJob job = new DataImportJob();
        job.setId("job-1");
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        Optional<DataImportJob> result = service.getJob("job-1");
        assertThat(result).isPresent();
    }
}
