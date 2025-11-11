package finance.project.api.dataimport.ingestion;

import finance.project.api.dataimport.DataImportJob;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    public void importGenericFile(DataImportJob job, Instant start, Instant end) {
        log.info("[CSV] Importing broker={} symbol={} timeframe={} range={} -> {}", job.getBroker(), job.getSymbol(), job.getTimeframe(), start, end);
        // Integration point: wire CME, custom CSV loaders, and shared persistence flows here.
    }
}
