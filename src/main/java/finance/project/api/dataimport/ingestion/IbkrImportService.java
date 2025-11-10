package finance.project.api.dataimport.ingestion;

import finance.project.api.dataimport.DataImportJob;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IbkrImportService {

    private static final Logger log = LoggerFactory.getLogger(IbkrImportService.class);

    public void fetchAndSave(DataImportJob job, Instant start, Instant end) {
        log.info("[IBKR] Fetching candles for symbol={} timeframe={} range={} -> {}", job.getSymbol(), job.getTimeframe(), start, end);
        // Integration point: call Interactive Brokers ingestion workflow when implemented.
    }
}
