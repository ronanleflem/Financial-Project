package finance.project.api.dataimport.dto;

import java.time.Instant;

public record DeltaIngestionRangeResponse(
        String symbol,
        String insertedType,
        Instant startDate,
        Instant endDate,
        String timeframe,
        Instant insertedAt
) {
}

