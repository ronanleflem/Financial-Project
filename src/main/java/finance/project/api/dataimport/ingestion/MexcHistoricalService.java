package finance.project.api.dataimport.ingestion;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MexcHistoricalService {

    private static final Logger log = LoggerFactory.getLogger(MexcHistoricalService.class);

    public void fetchAndSave(String symbol, String timeframe, Instant start, Instant end) {
        log.info("[MEXC] Fetching candles for symbol={} timeframe={} range={} -> {}", symbol, timeframe, start, end);
        // Integration point: call dedicated MEXC ingestion pipeline when available.
    }
}
