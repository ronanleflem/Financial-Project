package finance.project.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.quant.ApiJobEntity;
import finance.project.api.model.run.StressSourceRunItem;
import finance.project.api.model.run.StressSourceRunsResponse;
import finance.project.api.repositories.ApiJobRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class StressSourceRunService {
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final List<String> CANONICAL_JOB_TYPES = List.of("canonical_run", "run");
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_BATCHES = 12;
    private static final int MIN_FETCH_SIZE = 50;

    private final ApiJobRepository apiJobRepository;
    private final ObjectMapper objectMapper;

    public StressSourceRunService(ApiJobRepository apiJobRepository, ObjectMapper objectMapper) {
        this.apiJobRepository = apiJobRepository;
        this.objectMapper = objectMapper;
    }

    public StressSourceRunsResponse listEligibleSources(Integer requestedLimit, String cursor, String strategyType) {
        int limit = normalizeLimit(requestedLimit);
        String normalizedStrategyType = normalizeSpecType(strategyType);
        CursorToken cursorToken = CursorToken.decode(cursor);

        int fetchSize = Math.max(MIN_FETCH_SIZE, limit * 3);
        List<StressSourceRunItem> items = new ArrayList<>(limit);
        CursorToken nextCursor = null;

        for (int batch = 0; batch < MAX_BATCHES && items.size() < limit; batch++) {
            List<ApiJobEntity> page = fetchPage(cursorToken, fetchSize);
            if (page.isEmpty()) {
                nextCursor = null;
                break;
            }

            for (ApiJobEntity job : page) {
                nextCursor = CursorToken.from(job.getFinishedAt(), job.getJobId());
                ParsedFields fields = extractFields(job);

                if (!isEligibleSpecType(fields.specType())) {
                    continue;
                }
                if (normalizedStrategyType != null && !normalizedStrategyType.equals(fields.specType())) {
                    continue;
                }

                items.add(new StressSourceRunItem(
                        job.getJobId(),
                        fields.specType(),
                        job.getStatus(),
                        job.getCreatedAt(),
                        job.getFinishedAt(),
                        fields.symbol(),
                        fields.timeframe(),
                        fields.assetClass(),
                        fields.currency(),
                        fields.tradesCountEstimate()
                ));

                if (items.size() == limit) {
                    return new StressSourceRunsResponse(items, nextCursor == null ? null : nextCursor.encode());
                }
            }

            if (page.size() < fetchSize) {
                nextCursor = null;
                break;
            }
            cursorToken = nextCursor;
        }

        return new StressSourceRunsResponse(items, nextCursor == null ? null : nextCursor.encode());
    }

    private List<ApiJobEntity> fetchPage(CursorToken cursorToken, int fetchSize) {
        PageRequest pageRequest = PageRequest.of(0, fetchSize);
        if (cursorToken == null) {
            return apiJobRepository.findByJobTypeInAndStatusOrderByFinishedAtDescJobIdDesc(
                    CANONICAL_JOB_TYPES,
                    STATUS_SUCCEEDED,
                    pageRequest
            );
        }
        if (cursorToken.finishedAt() == null) {
            return apiJobRepository.findAfterCursorWithNullFinishedAt(
                    CANONICAL_JOB_TYPES,
                    STATUS_SUCCEEDED,
                    cursorToken.jobId(),
                    pageRequest
            );
        }
        return apiJobRepository.findAfterCursorWithNonNullFinishedAt(
                CANONICAL_JOB_TYPES,
                STATUS_SUCCEEDED,
                cursorToken.finishedAt(),
                cursorToken.jobId(),
                pageRequest
        );
    }

    private ParsedFields extractFields(ApiJobEntity job) {
        JsonNode payload = parse(job.getPayloadJson());
        JsonNode result = parse(job.getResultJson());

        String specType = firstNonBlank(
                readText(payload, "spec_type"),
                readText(payload, "specType"),
                readText(path(payload, "spec"), "spec_type"),
                readText(path(payload, "spec"), "specType"),
                readText(result, "spec_type"),
                readText(result, "specType"),
                readText(path(result, "spec"), "spec_type")
        );
        specType = normalizeSpecType(specType);

        String symbol = firstNonBlank(
                readPathText(payload, "data", "symbol"),
                readText(payload, "symbol"),
                readPathText(result, "meta", "symbol"),
                readPathText(result, "result", "meta", "symbol")
        );
        String timeframe = firstNonBlank(
                readPathText(payload, "data", "timeframe"),
                readText(payload, "timeframe"),
                readPathText(result, "meta", "timeframe"),
                readPathText(result, "result", "meta", "timeframe")
        );
        String assetClass = firstNonBlank(
                readPathText(payload, "data", "asset_class"),
                readPathText(payload, "data", "assetClass"),
                readText(payload, "asset_class"),
                readText(payload, "assetClass"),
                readPathText(result, "meta", "asset_class"),
                readPathText(result, "meta", "assetClass"),
                readPathText(result, "result", "meta", "asset_class")
        );
        String currency = firstNonBlank(
                readPathText(payload, "data", "currency"),
                readText(payload, "currency"),
                readPathText(result, "meta", "currency"),
                readPathText(result, "result", "meta", "currency")
        );
        Integer tradesCountEstimate = firstNonNull(
                readInt(payload, "trades_count_estimate"),
                readInt(payload, "tradesCountEstimate"),
                readPathInt(result, "summary", "trades_count"),
                readPathInt(result, "summary", "tradesCount"),
                readPathInt(result, "result", "summary", "trades_count")
        );

        return new ParsedFields(specType, symbol, timeframe, assetClass, currency, tradesCountEstimate);
    }

    private JsonNode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private String readText(JsonNode root, String field) {
        if (root == null || field == null || field.isBlank()) {
            return null;
        }
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer readInt(JsonNode root, String field) {
        if (root == null) {
            return null;
        }
        JsonNode node = root.get(field);
        return parseInt(node);
    }

    private String readPathText(JsonNode root, String... path) {
        JsonNode node = path(root, path);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer readPathInt(JsonNode root, String... path) {
        JsonNode node = path(root, path);
        return parseInt(node);
    }

    private JsonNode path(JsonNode root, String... path) {
        if (root == null || path == null || path.length == 0) {
            return null;
        }
        JsonNode cursor = root;
        for (String segment : path) {
            if (cursor == null) {
                return null;
            }
            cursor = cursor.get(segment);
        }
        return cursor;
    }

    private Integer parseInt(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.intValue();
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText().trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private static boolean isEligibleSpecType(String specType) {
        return "dca".equals(specType) || "backtest".equals(specType);
    }

    private static String normalizeSpecType(String specType) {
        if (specType == null) {
            return null;
        }
        String normalized = specType.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record ParsedFields(
            String specType,
            String symbol,
            String timeframe,
            String assetClass,
            String currency,
            Integer tradesCountEstimate
    ) {
    }

    static record CursorToken(Instant finishedAt, String jobId) {
        static CursorToken from(Instant finishedAt, String jobId) {
            return new CursorToken(finishedAt, jobId);
        }

        static CursorToken decode(String cursor) {
            if (cursor == null || cursor.isBlank()) {
                return null;
            }
            try {
                byte[] decoded = Base64.getUrlDecoder().decode(cursor);
                String raw = new String(decoded, StandardCharsets.UTF_8);
                String[] parts = raw.split("\\|", 2);
                if (parts.length != 2 || parts[1].isBlank()) {
                    return null;
                }
                Instant finishedAt = "-".equals(parts[0]) ? null : Instant.parse(parts[0]);
                return new CursorToken(finishedAt, parts[1]);
            } catch (Exception ignored) {
                return null;
            }
        }

        String encode() {
            String left = finishedAt == null ? "-" : finishedAt.toString();
            String raw = left + "|" + jobId;
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }
    }
}
