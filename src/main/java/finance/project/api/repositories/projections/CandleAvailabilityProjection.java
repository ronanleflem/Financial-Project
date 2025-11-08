package finance.project.api.repositories.projections;

import java.time.LocalDateTime;

public interface CandleAvailabilityProjection {
    String getSymbol();
    String getBroker();
    String getTimeframe();
    LocalDateTime getStart();
    LocalDateTime getEnd();
    Long getCount();
    LocalDateTime getUpdatedAt();
    String getMarketType();
}
