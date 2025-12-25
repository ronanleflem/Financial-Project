package finance.project.api.dataimport.ingestion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import finance.project.api.dataimport.DataImportJob;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CsvImportServiceTest {

    @Test
    void importGenericFileDoesNotThrow() {
        CsvImportService service = new CsvImportService();
        DataImportJob job = new DataImportJob();
        job.setBroker("CSV");
        job.setSymbol("ES");
        job.setTimeframe("1min");

        assertThatCode(() -> service.importGenericFile(job, Instant.EPOCH, Instant.EPOCH.plusSeconds(60)))
                .doesNotThrowAnyException();
    }

    @Test
    void importGenericFileDoesNotThrowWhenJobFieldsMissing() {
        CsvImportService service = new CsvImportService();
        DataImportJob job = new DataImportJob();

        assertThatCode(() -> service.importGenericFile(job, Instant.EPOCH, Instant.EPOCH.plusSeconds(60)))
                .doesNotThrowAnyException();
    }

    @Test
    void importGenericFileThrowsWhenJobIsNull() {
        CsvImportService service = new CsvImportService();

        assertThatThrownBy(() -> service.importGenericFile(null, Instant.EPOCH, Instant.EPOCH.plusSeconds(60)))
                .isInstanceOf(NullPointerException.class);
    }
}
