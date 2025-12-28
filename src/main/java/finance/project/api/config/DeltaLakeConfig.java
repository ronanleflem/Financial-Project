package finance.project.api.config;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Centralizes configuration for Delta Lake interactions.
 */
@Configuration
public class DeltaLakeConfig {

    private final String baseUri;

    public DeltaLakeConfig(@Value("${delta.base-uri}") String baseUri) {
        Assert.hasText(baseUri, "delta.base-uri must be configured");
        this.baseUri = stripTrailingSlash(baseUri.trim());
    }

    public String getBaseUri() {
        return baseUri;
    }

    public String resolveTablePath(String assetCategory, String broker, String venue, String symbol) {
        Assert.hasText(assetCategory, "assetCategory must not be blank");
        Assert.hasText(symbol, "symbol must not be blank");

        String sanitizedCategory = sanitizeSegment(assetCategory);
        String sanitizedBroker = sanitizeSegment(broker);
        String sanitizedVenue = sanitizeSegment(venue);
        SymbolPathSegments segments = parseSymbolSegments(symbol);

        return baseUri + "/" + sanitizedCategory + "/" + sanitizedBroker + "/" + sanitizedVenue + "/"
                + segments.currency() + "/" + segments.baseSymbol() + "/";
    }

    private SymbolPathSegments parseSymbolSegments(String symbol) {
        String trimmed = StringUtils.trimAllWhitespace(symbol).toUpperCase(Locale.ROOT);
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            return new SymbolPathSegments(sanitizeSegment(parts[0]), sanitizeSegment(parts[1]));
        }
        if (trimmed.contains("_")) {
            String[] parts = trimmed.split("_", 2);
            return new SymbolPathSegments(sanitizeSegment(parts[0]), sanitizeSegment(parts[1]));
        }

        String base = trimmed;
        String currency = "UNKNOWN";
        for (String quote : KNOWN_QUOTES) {
            if (trimmed.endsWith(quote) && trimmed.length() > quote.length()) {
                base = trimmed.substring(0, trimmed.length() - quote.length());
                currency = quote;
                break;
            }
        }

        return new SymbolPathSegments(sanitizeSegment(base), sanitizeSegment(currency));
    }

    private String sanitizeSegment(String value) {
        if (value == null) {
            return "UNKNOWN";
        }
        String sanitized = StringUtils.trimWhitespace(value).replaceAll("[^a-zA-Z0-9_-]", "_");
        return sanitized.isEmpty() ? "UNKNOWN" : sanitized.toUpperCase();
    }

    private record SymbolPathSegments(String baseSymbol, String currency) {}

    private static final List<String> KNOWN_QUOTES = List.of(
            "USDT", "USDC", "BTC", "ETH",
            "EUR", "USD", "GBP", "CAD",
            "AUD", "JPY", "CHF"
    );

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
