package finance.project.api.services;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.DataImportJobRepository;
import finance.project.api.model.CandleAvailabilityDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.projections.CandleAvailabilityProjection;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CandleAvailabilityService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final CandleRepository candleRepository;
    private final DataImportJobRepository jobRepository;

    public CandleAvailabilityService(CandleRepository candleRepository,
                                     DataImportJobRepository jobRepository) {
        this.candleRepository = candleRepository;
        this.jobRepository = jobRepository;
    }

    public List<CandleAvailabilityDTO> getAvailability(String symbol,
                                                        String timeframe,
                                                        String broker,
                                                        String marketType) {
        List<CandleAvailabilityDTO> fromDb = new ArrayList<>();
        for (CandleAvailabilityProjection projection : candleRepository.findAvailabilitySummary()) {
            if (!matchesFilters(projection.getSymbol(), symbol)) {
                continue;
            }
            if (!matchesFilters(projection.getTimeframe(), timeframe)) {
                continue;
            }
            if (!matchesFilters(projection.getBroker(), broker)) {
                continue;
            }
            if (!matchesFilters(projection.getMarketType(), marketType)) {
                continue;
            }
            CandleAvailabilityDTO dto = mapProjection(projection);
            fromDb.add(dto);
        }

        if (jobRepository != null) {
            appendJobs(fromDb, symbol, timeframe, broker, marketType);
        }

        return fromDb;
    }

    private void appendJobs(List<CandleAvailabilityDTO> existing,
                            String symbol,
                            String timeframe,
                            String broker,
                            String marketType) {
        for (DataImportJob job : jobRepository.findByStatus(DataImportJob.Status.SUCCESS)) {
            if (!matchesFilters(job.getSymbol(), symbol)) {
                continue;
            }
            if (!matchesFilters(job.getTimeframe(), timeframe)) {
                continue;
            }
            if (!matchesFilters(job.getBroker(), broker)) {
                continue;
            }
            if (!matchesFilters(job.getVenue(), marketType)) {
                continue;
            }
            boolean dbSeriesExists = existing.stream().anyMatch(dto ->
                    equalsIgnoreCase(dto.symbol(), job.getSymbol())
                            && equalsIgnoreCase(dto.timeframe(), job.getTimeframe())
                            && equalsIgnoreCase(dto.source(), "DB"));
            if (dbSeriesExists) {
                continue;
            }
            boolean jobAlreadyListed = existing.stream().anyMatch(dto ->
                    equalsIgnoreCase(dto.symbol(), job.getSymbol())
                            && equalsIgnoreCase(dto.timeframe(), job.getTimeframe())
                            && equalsIgnoreCase(dto.broker(), job.getBroker())
                            && equalsIgnoreCase(dto.source(), "JOB"));
            if (jobAlreadyListed) {
                continue;
            }
            existing.add(new CandleAvailabilityDTO(
                    job.getSymbol(),
                    job.getBroker(),
                    job.getTimeframe(),
                    formatInstant(job.getStartDate()),
                    formatInstant(job.getEndDate()),
                    0L,
                    null,
                    null,
                    null,
                    job.getTimezone() != null ? job.getTimezone() : "UTC",
                    "JOB",
                    job.getVenue(),
                    formatInstant(job.getUpdatedAt())
            ));
        }
    }

    private CandleAvailabilityDTO mapProjection(CandleAvailabilityProjection projection) {
        LocalDateTime start = projection.getStart();
        LocalDateTime end = projection.getEnd();
        long count = projection.getCount() != null ? projection.getCount() : 0L;
        CoverageStats stats = computeCoverage(start, end, projection.getTimeframe(), count);
        return new CandleAvailabilityDTO(
                projection.getSymbol(),
                projection.getBroker(),
                projection.getTimeframe(),
                formatDateTime(start),
                formatDateTime(end),
                count,
                stats.coveragePct(),
                stats.gapsPct(),
                null,
                "UTC",
                "DB",
                projection.getMarketType(),
                formatDateTime(projection.getUpdatedAt())
        );
    }

    private static String formatDateTime(LocalDateTime value) {
        return value != null ? DATE_TIME_FORMATTER.format(value) : null;
    }

    private static String formatInstant(Instant value) {
        return value != null ? INSTANT_FORMATTER.format(value) : null;
    }

    private CoverageStats computeCoverage(LocalDateTime start,
                                          LocalDateTime end,
                                          String timeframe,
                                          long count) {
        if (count <= 0) {
            return new CoverageStats(0.0, 100.0);
        }
        Duration tfDuration = resolveTimeframeDuration(timeframe);
        if (tfDuration == null) {
            return new CoverageStats(null, null);
        }
        if (start == null || end == null) {
            return new CoverageStats(null, null);
        }
        long tfSeconds = tfDuration.getSeconds();
        if (tfSeconds <= 0) {
            return new CoverageStats(null, null);
        }
        long spanSeconds = Math.max(0L, Duration.between(start, end).getSeconds());
        long expected = (spanSeconds / tfSeconds) + 1;
        if (expected <= 0) {
            expected = 1;
        }
        double ratio = Math.min(1.0, count / (double) expected);
        double coverage = toPercentage(ratio);
        double gaps = toPercentage(1.0 - ratio);
        return new CoverageStats(coverage, gaps);
    }

    private static double toPercentage(double value) {
        double pct = value * 100.0;
        return Math.round(pct * 100.0) / 100.0;
    }

    private Duration resolveTimeframeDuration(String timeframe) {
        if (!StringUtils.hasText(timeframe)) {
            return null;
        }
        String normalized = timeframe.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1min" -> Duration.ofMinutes(1);
            case "3min" -> Duration.ofMinutes(3);
            case "5min" -> Duration.ofMinutes(5);
            case "10min" -> Duration.ofMinutes(10);
            case "15min" -> Duration.ofMinutes(15);
            case "30min" -> Duration.ofMinutes(30);
            case "45min" -> Duration.ofMinutes(45);
            case "1h" -> Duration.ofHours(1);
            case "2h" -> Duration.ofHours(2);
            case "4h" -> Duration.ofHours(4);
            case "8h" -> Duration.ofHours(8);
            case "12h" -> Duration.ofHours(12);
            case "daily", "1d" -> Duration.ofDays(1);
            case "weekly", "1w" -> Duration.ofDays(7);
            case "monthly", "1mo" -> Duration.ofDays(30);
            default -> parseDynamicDuration(normalized);
        };
    }

    private Duration parseDynamicDuration(String normalized) {
        if (normalized.endsWith("mo")) {
            return parseDuration(normalized, "mo", 30L * 24L * 3600L);
        }
        if (normalized.endsWith("min")) {
            return parseDuration(normalized, "min", 60L);
        }
        if (normalized.endsWith("m")) {
            return parseDuration(normalized, "m", 60L);
        }
        if (normalized.endsWith("h")) {
            return parseDuration(normalized, "h", 3600L);
        }
        if (normalized.endsWith("d")) {
            return parseDuration(normalized, "d", 24L * 3600L);
        }
        if (normalized.endsWith("w")) {
            return parseDuration(normalized, "w", 7L * 24L * 3600L);
        }
        return null;
    }

    private Duration parseDuration(String value, String suffix, long unitInSeconds) {
        String numberPart = value.substring(0, value.length() - suffix.length());
        try {
            long quantity = Long.parseLong(numberPart);
            if (quantity <= 0) {
                return null;
            }
            return Duration.ofSeconds(quantity * unitInSeconds);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean matchesFilters(String value, String filter) {
        if (!StringUtils.hasText(filter)) {
            return true;
        }
        return StringUtils.hasText(value) && value.equalsIgnoreCase(filter);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) {
            return true;
        }
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.equalsIgnoreCase(right);
    }

    private record CoverageStats(Double coveragePct, Double gapsPct) {
    }
}
