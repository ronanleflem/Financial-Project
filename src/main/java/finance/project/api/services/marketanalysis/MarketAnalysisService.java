package finance.project.api.services.marketanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.quant.ApiJobEntity;
import finance.project.api.entities.quant.MarketStatsEntity;
import finance.project.api.entities.quant.SeasonalityProfileEntity;
import finance.project.api.entities.quant.SeasonalityRunEntity;
import finance.project.api.model.marketanalysis.MarketAnalysisMarketStatsRow;
import finance.project.api.model.marketanalysis.MarketAnalysisResultData;
import finance.project.api.model.marketanalysis.MarketAnalysisResultMeta;
import finance.project.api.model.marketanalysis.MarketAnalysisRunDetailResponse;
import finance.project.api.model.marketanalysis.MarketAnalysisRunItem;
import finance.project.api.model.marketanalysis.MarketAnalysisRunListResponse;
import finance.project.api.model.marketanalysis.MarketAnalysisRunResultResponse;
import finance.project.api.model.marketanalysis.MarketAnalysisSeasonalityProfileRow;
import finance.project.api.model.marketanalysis.MarketAnalysisSeasonalityRunSummary;
import finance.project.api.repositories.ApiJobRepository;
import finance.project.api.repositories.MarketStatsRepository;
import finance.project.api.repositories.SeasonalityProfileRepository;
import finance.project.api.repositories.SeasonalityRunRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class MarketAnalysisService {
    private static final String CANONICAL_RUN = "canonical_run";
    private static final Set<String> TERMINAL_STATUSES = Set.of(
            "done", "completed", "failed", "cancelled", "canceled", "succeeded", "success"
    );

    private final ApiJobRepository apiJobRepository;
    private final MarketStatsRepository marketStatsRepository;
    private final SeasonalityRunRepository seasonalityRunRepository;
    private final SeasonalityProfileRepository seasonalityProfileRepository;
    private final ObjectMapper objectMapper;

    public MarketAnalysisService(ApiJobRepository apiJobRepository,
                                 MarketStatsRepository marketStatsRepository,
                                 SeasonalityRunRepository seasonalityRunRepository,
                                 SeasonalityProfileRepository seasonalityProfileRepository,
                                 ObjectMapper objectMapper) {
        this.apiJobRepository = apiJobRepository;
        this.marketStatsRepository = marketStatsRepository;
        this.seasonalityRunRepository = seasonalityRunRepository;
        this.seasonalityProfileRepository = seasonalityProfileRepository;
        this.objectMapper = objectMapper;
    }

    public MarketAnalysisRunListResponse listRuns(String specType,
                                                  String status,
                                                  String symbol,
                                                  String timeframe,
                                                  Instant from,
                                                  Instant to,
                                                  int page,
                                                  int size,
                                                  String sort) {
        Sort dbSort = toSort(sort);
        List<ApiJobEntity> all = apiJobRepository.findByJobType(CANONICAL_RUN, dbSort);

        List<IndexedRun> filtered = all.stream()
                .map(this::toIndexedRun)
                .filter(item -> specType.equals(item.specType()))
                .filter(item -> status == null || status.isBlank() || equalsIgnoreCase(item.status(), status))
                .filter(item -> symbol == null || symbol.isBlank() || equalsIgnoreCase(item.symbol(), symbol))
                .filter(item -> timeframe == null || timeframe.isBlank() || equalsIgnoreCase(item.timeframe(), timeframe))
                .filter(item -> from == null || (item.createdAt() != null && !item.createdAt().isBefore(from)))
                .filter(item -> to == null || (item.createdAt() != null && !item.createdAt().isAfter(to)))
                .toList();

        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int fromIndex = safePage * safeSize;
        int toIndex = Math.min(filtered.size(), fromIndex + safeSize);
        List<MarketAnalysisRunItem> items = fromIndex >= filtered.size()
                ? List.of()
                : filtered.subList(fromIndex, toIndex).stream().map(IndexedRun::item).toList();
        int totalPages = (int) Math.ceil((double) filtered.size() / safeSize);

        return new MarketAnalysisRunListResponse(items, safePage, safeSize, filtered.size(), totalPages, sort == null ? "" : sort);
    }

    public MarketAnalysisRunDetailResponse getRunDetail(String runId) {
        ApiJobEntity job = apiJobRepository.findByJobId(runId)
                .orElseThrow(() -> new MarketAnalysisNotFoundException("run not found: " + runId));

        JsonNode payload = parseJson(job.getPayloadJson());
        JsonNode progress = parseJson(job.getProgressJson());
        JsonNode resultJson = parseJson(job.getResultJson());
        String specType = extractSpecType(payload);
        String requestId = firstNonBlank(readText(payload, "request_id"), readText(payload, "request", "request_id"), job.getJobId());
        String specId = extractSpecId(payload, resultJson);
        String datasetId = extractDatasetId(payload, resultJson);

        return new MarketAnalysisRunDetailResponse(
                job.getJobId(),
                requestId,
                specType,
                job.getStatus(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getUpdatedAt(),
                job.getErrorMessage(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getTimeoutSeconds(),
                job.getCancelRequested(),
                job.getCanceledAt(),
                payload,
                progress,
                hasParsedResultJson(job),
                extractPersistenceEnabled(payload),
                specId,
                datasetId
        );
    }

    public MarketAnalysisRunResultResponse getRunResult(String runId) {
        ApiJobEntity job = apiJobRepository.findByJobId(runId)
                .orElseThrow(() -> new MarketAnalysisNotFoundException("run not found: " + runId));

        JsonNode payload = parseJson(job.getPayloadJson());
        JsonNode resultJson = parseJson(job.getResultJson());
        String status = normalizeStatus(job.getStatus());
        boolean terminal = isTerminalStatus(job.getStatus());
        if (!terminal && resultJson == null) {
            throw new MarketAnalysisResultNotReadyException("result not ready for run: " + runId);
        }

        String specType = extractSpecType(payload);
        String specId = extractSpecId(payload, resultJson);
        String datasetId = extractDatasetId(payload, resultJson);

        if ("market_stats".equals(specType)) {
            List<MarketStatsEntity> rows = loadMarketStats(specId, datasetId);
            if (!rows.isEmpty()) {
                String start = rows.stream().map(MarketStatsEntity::getStart).filter(s -> s != null && !s.isBlank()).min(String::compareTo).orElse(null);
                String end = rows.stream().map(MarketStatsEntity::getEnd).filter(s -> s != null && !s.isBlank()).max(String::compareTo).orElse(null);
                return new MarketAnalysisRunResultResponse(
                        runId,
                        specType,
                        "persisted_tables",
                        new MarketAnalysisResultMeta(specId, datasetId, null, null, start, end, status),
                        new MarketAnalysisResultData(
                                rows.stream().map(this::toMarketStatsRow).toList(),
                                List.of(),
                                null,
                                null
                        )
                );
            }
        }

        if ("seasonality".equals(specType)) {
            SeasonalityRunEntity run = loadSeasonalityRun(runId, specId, datasetId);
            List<SeasonalityProfileEntity> profiles = loadSeasonalityProfiles(specId, datasetId);
            if (run != null || !profiles.isEmpty()) {
                JsonNode bestSummary = run == null ? null : parseJson(run.getBestSummary());
                String start = profiles.stream().map(SeasonalityProfileEntity::getStart).filter(s -> s != null && !s.isBlank()).min(String::compareTo).orElse(null);
                String end = profiles.stream().map(SeasonalityProfileEntity::getEnd).filter(s -> s != null && !s.isBlank()).max(String::compareTo).orElse(null);
                return new MarketAnalysisRunResultResponse(
                        runId,
                        specType,
                        "persisted_tables",
                        new MarketAnalysisResultMeta(
                                firstNonBlank(specId, run == null ? null : run.getSpecId()),
                                firstNonBlank(datasetId, run == null ? null : run.getDatasetId()),
                                run == null ? null : run.getOutDir(),
                                readText(payload, "data", "window"),
                                start,
                                end,
                                run == null ? status : run.getStatus()
                        ),
                        new MarketAnalysisResultData(
                                List.of(),
                                profiles.stream().map(this::toSeasonalityProfileRow).toList(),
                                run == null ? null : new MarketAnalysisSeasonalityRunSummary(
                                        run.getRunId(),
                                        run.getSpecId(),
                                        run.getDatasetId(),
                                        run.getOutDir(),
                                        run.getStatus(),
                                        bestSummary,
                                        run.getCreatedAt()
                                ),
                                null
                        )
                );
            }
        }

        return new MarketAnalysisRunResultResponse(
                runId,
                specType,
                "result_json",
                new MarketAnalysisResultMeta(specId, datasetId, null, readText(payload, "data", "window"), null, null, status),
                new MarketAnalysisResultData(List.of(), List.of(), null, resultJson)
        );
    }

    private boolean hasParsedResultJson(ApiJobEntity job) {
        return parseJson(job.getResultJson()) != null;
    }

    private List<MarketStatsEntity> loadMarketStats(String specId, String datasetId) {
        if (specId != null && datasetId != null) {
            return marketStatsRepository.findBySpecIdAndDatasetIdOrderByCreatedAtAsc(specId, datasetId);
        }
        if (specId != null) {
            return marketStatsRepository.findBySpecIdOrderByCreatedAtAsc(specId);
        }
        if (datasetId != null) {
            return marketStatsRepository.findByDatasetIdOrderByCreatedAtAsc(datasetId);
        }
        return List.of();
    }

    private SeasonalityRunEntity loadSeasonalityRun(String runId, String specId, String datasetId) {
        SeasonalityRunEntity byRun = seasonalityRunRepository.findByRunId(runId).orElse(null);
        if (byRun != null) {
            return byRun;
        }
        if (specId != null && datasetId != null) {
            return seasonalityRunRepository.findBySpecIdAndDatasetIdOrderByCreatedAtDesc(specId, datasetId).stream().findFirst().orElse(null);
        }
        if (specId != null) {
            return seasonalityRunRepository.findBySpecIdOrderByCreatedAtDesc(specId).stream().findFirst().orElse(null);
        }
        if (datasetId != null) {
            return seasonalityRunRepository.findByDatasetIdOrderByCreatedAtDesc(datasetId).stream().findFirst().orElse(null);
        }
        return null;
    }

    private List<SeasonalityProfileEntity> loadSeasonalityProfiles(String specId, String datasetId) {
        if (specId != null && datasetId != null) {
            return seasonalityProfileRepository.findBySpecIdAndDatasetIdOrderByCreatedAtAsc(specId, datasetId);
        }
        if (specId != null) {
            return seasonalityProfileRepository.findBySpecIdOrderByCreatedAtAsc(specId);
        }
        if (datasetId != null) {
            return seasonalityProfileRepository.findByDatasetIdOrderByCreatedAtAsc(datasetId);
        }
        return List.of();
    }

    private IndexedRun toIndexedRun(ApiJobEntity job) {
        JsonNode payload = parseJson(job.getPayloadJson());
        JsonNode result = parseJson(job.getResultJson());
        MarketAnalysisRunItem item = new MarketAnalysisRunItem(
                job.getJobId(),
                firstNonBlank(readText(payload, "request_id"), readText(payload, "request", "request_id"), job.getJobId()),
                extractSpecType(payload),
                job.getStatus(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getUpdatedAt(),
                job.getErrorMessage(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getCancelRequested(),
                extractPersistenceEnabled(payload),
                extractSpecId(payload, result),
                extractDatasetId(payload, result)
        );
        return new IndexedRun(
                item,
                firstNonBlank(readText(payload, "request", "data", "symbol"), readText(payload, "data", "symbol")),
                firstNonBlank(readText(payload, "request", "data", "timeframe"), readText(payload, "data", "timeframe"))
        );
    }

    private MarketAnalysisMarketStatsRow toMarketStatsRow(MarketStatsEntity e) {
        return new MarketAnalysisMarketStatsRow(
                e.getSymbol(),
                e.getTimeframe(),
                e.getEvent(),
                e.getConditionName(),
                e.getConditionValue(),
                e.getTarget(),
                e.getSplit(),
                e.getN(),
                e.getSuccesses(),
                e.getPHat(),
                e.getCiLow(),
                e.getCiHigh(),
                e.getLift(),
                e.getPMean(),
                e.getPMap(),
                e.getHdiLow(),
                e.getHdiHigh(),
                e.getLiftFreq(),
                e.getLiftBayes(),
                e.getPValue(),
                e.getQValue(),
                e.getSignificant(),
                e.getInsufficient(),
                e.getStart(),
                e.getEnd(),
                e.getSpecId(),
                e.getDatasetId(),
                e.getCreatedAt()
        );
    }

    private MarketAnalysisSeasonalityProfileRow toSeasonalityProfileRow(SeasonalityProfileEntity e) {
        return new MarketAnalysisSeasonalityProfileRow(
                e.getSymbol(),
                e.getTimeframe(),
                e.getDim(),
                e.getBin(),
                e.getMeasure(),
                e.getScore(),
                e.getN(),
                e.getBaseline(),
                e.getLift(),
                parseJson(e.getMetrics()),
                e.getStart(),
                e.getEnd(),
                e.getSpecId(),
                e.getDatasetId(),
                e.getCreatedAt()
        );
    }

    private Sort toSort(String sort) {
        String raw = (sort == null || sort.isBlank()) ? "created_at,desc" : sort;
        String[] parts = raw.split(",");
        String key = parts[0].trim().toLowerCase(Locale.ROOT);
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, mapSortField(key));
    }

    private String mapSortField(String key) {
        return switch (key) {
            case "created_at" -> "createdAt";
            case "started_at" -> "startedAt";
            case "finished_at" -> "finishedAt";
            case "updated_at" -> "updatedAt";
            case "status" -> "status";
            default -> "createdAt";
        };
    }

    private String extractSpecType(JsonNode payload) {
        return firstNonBlank(readText(payload, "request", "spec_type"), readText(payload, "spec_type"), readText(payload, "specType"));
    }

    private Boolean extractPersistenceEnabled(JsonNode payload) {
        Boolean value = readBoolean(payload, "request", "persistence", "enabled");
        if (value != null) {
            return value;
        }
        return readBoolean(payload, "persistence", "enabled");
    }

    private String extractSpecId(JsonNode payload, JsonNode result) {
        return firstNonBlank(
                readText(result, "spec_id"),
                readText(result, "meta", "spec_id"),
                readText(payload, "request", "spec_id"),
                readText(payload, "spec_id")
        );
    }

    private String extractDatasetId(JsonNode payload, JsonNode result) {
        return firstNonBlank(
                readText(result, "dataset_id"),
                readText(result, "meta", "dataset_id"),
                readText(payload, "request", "dataset_id"),
                readText(payload, "dataset_id")
        );
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private boolean isTerminalStatus(String status) {
        return TERMINAL_STATUSES.contains(normalizeStatus(status));
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readText(JsonNode root, String... path) {
        if (root == null || path == null) {
            return null;
        }
        JsonNode cursor = root;
        for (String p : path) {
            if (cursor == null) {
                return null;
            }
            cursor = cursor.get(p);
        }
        if (cursor == null || cursor.isNull()) {
            return null;
        }
        String value = cursor.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Boolean readBoolean(JsonNode root, String... path) {
        if (root == null || path == null) {
            return null;
        }
        JsonNode cursor = root;
        for (String p : path) {
            if (cursor == null) {
                return null;
            }
            cursor = cursor.get(p);
        }
        if (cursor == null || cursor.isNull()) {
            return null;
        }
        return cursor.asBoolean();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private record IndexedRun(
            MarketAnalysisRunItem item,
            String symbol,
            String timeframe
    ) {
        String specType() {
            return item.specType();
        }

        String status() {
            return item.status();
        }

        Instant createdAt() {
            return item.createdAt();
        }
    }
}
