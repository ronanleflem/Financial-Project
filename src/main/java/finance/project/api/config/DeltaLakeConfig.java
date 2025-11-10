package finance.project.api.config;

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

    public String resolveTablePath(String assetCategory, String symbol) {
        Assert.hasText(assetCategory, "assetCategory must not be blank");
        Assert.hasText(symbol, "symbol must not be blank");

        String sanitizedCategory = sanitizeSegment(assetCategory);
        String sanitizedSymbol = sanitizeSegment(symbol);

        return baseUri + "/" + sanitizedCategory + "/" + sanitizedSymbol + "/";
    }

    private String sanitizeSegment(String value) {
        String sanitized = StringUtils.trimAllWhitespace(value).replaceAll("[^a-zA-Z0-9_-]", "_");
        return sanitized.isEmpty() ? "UNKNOWN" : sanitized.toUpperCase();
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
