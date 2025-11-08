package finance.project.api.dataimport.ingestion;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IbkrImportService {

    private static final Logger log = LoggerFactory.getLogger(IbkrImportService.class);

    public void fetchAndSave(String symbol, String timeframe, Instant start, Instant end) {
        log.info("[IBKR] Fetching candles for symbol={} timeframe={} range={} -> {}", symbol, timeframe, start, end);
        // Integration point: call Interactive Brokers ingestion workflow when implemented.
    }
}
