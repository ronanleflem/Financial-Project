package finance.project.api.dataimport.ingestion;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    public void importGenericFile(String broker, String symbol, String timeframe, Instant start, Instant end) {
        log.info("[CSV] Importing broker={} symbol={} timeframe={} range={} -> {}", broker, symbol, timeframe, start, end);
        // Integration point: wire CME, custom CSV loaders, and shared persistence flows here.
    }
}
