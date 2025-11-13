package finance.project.api.universe.client;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FmbUniverseClient {

    private static final Logger log = LoggerFactory.getLogger(FmbUniverseClient.class);

    public List<String> fetchIndexMembers(String universeCode) {
        log.info("[FMB] Fetching members for universe {}", universeCode);
        // TODO: replace with real HTTP call to the FMB/FMP API.
        return switch (universeCode.toUpperCase()) {
            case "NASDAQ100" -> List.of("AAPL", "MSFT", "GOOGL", "AMZN", "NVDA");
            case "CAC40" -> List.of("MC.PA", "OR.PA", "SAN.PA", "BNP.PA");
            case "SP500", "S&P500", "SPX" -> List.of("AAPL", "MSFT", "GOOGL", "AMZN", "META");
            default -> List.of("AAPL", "MSFT");
        };
    }
}
