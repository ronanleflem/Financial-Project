package finance.project.api.dataimport.ingestion;

import finance.project.api.dataimport.DataImportJob;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MexcHistoricalService {

    private static final Logger log = LoggerFactory.getLogger(MexcHistoricalService.class);

    public void fetchAndSave(DataImportJob job, Instant start, Instant end) {
        log.info("[MEXC] Fetching candles for symbol={} timeframe={} range={} -> {}", job.getSymbol(), job.getTimeframe(), start, end);
        // Integration point: call dedicated MEXC ingestion pipeline when available.
    }
}
