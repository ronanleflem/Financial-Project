package finance.project.api.utils;

import finance.project.api.config.DeltaLakeConfig;
import finance.project.api.ibkr.model.ResolvedInstrument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class DeltaPathBuilder {

    private final DeltaLakeConfig deltaLakeConfig;

    public DeltaPathBuilder(DeltaLakeConfig deltaLakeConfig) {
        this.deltaLakeConfig = deltaLakeConfig;
    }

    public String buildPath(ResolvedInstrument instrument) {
        String market = sanitize(instrument != null ? instrument.marketTypeNormalized() : "UNKNOWN");
        String exchange = sanitize(instrument != null ? instrument.normalizedExchange() : "UNKNOWN");
        String currency = sanitize(instrument != null ? instrument.currency() : "UNKNOWN");
        String symbol = sanitize(instrument != null ? instrument.symbol() : "UNKNOWN");
        return "%s/%s/%s/%s/%s".formatted(deltaLakeConfig.getBaseUri(), market, exchange, currency, symbol);
    }

    private String sanitize(String value) {
        if (value == null) {
            return "UNKNOWN";
        }
        String cleaned = StringUtils.trimWhitespace(value)
                .replace('\\', '_')
                .replace('/', '_')
                .replaceAll("\\s+", "_")
                .toUpperCase(Locale.ROOT);
        cleaned = cleaned.replaceAll("[^A-Z0-9._-]", "_");
        if (cleaned.isBlank()) {
            return "UNKNOWN";
        }
        return cleaned;
    }
}
