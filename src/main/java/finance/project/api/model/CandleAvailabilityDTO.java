package finance.project.api.model;

public record CandleAvailabilityDTO(
        String symbol,
        String broker,
        String timeframe,
        String start,
        String end,
        long count,
        Double coveragePct,
        Double gapsPct,
        String sessions,
        String tz,
        String source,
        String marketType,
        String updatedAt
) {
}
