package finance.project.api.dataimport.dto;

import finance.project.api.dataimport.DataImportJob;
import java.time.Instant;

public record DataImportJobResponse(
        String id,
        String broker,
        String symbol,
        String timeframe,
        Instant startDate,
        Instant endDate,
        String sourceType,
        String venue,
        String timezone,
        String conflictPolicy,
        String rollover,
        String status,
        int progress,
        String message,
        Instant createdAt,
        Instant updatedAt
) {
    public static DataImportJobResponse fromEntity(DataImportJob job) {
        return new DataImportJobResponse(
                job.getId(),
                job.getBroker(),
                job.getSymbol(),
                job.getTimeframe(),
                job.getStartDate(),
                job.getEndDate(),
                job.getSourceType(),
                job.getVenue(),
                job.getTimezone(),
                job.getConflictPolicy(),
                job.getRollover(),
                job.getStatus() != null ? job.getStatus().name() : null,
                job.getProgress(),
                job.getMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
