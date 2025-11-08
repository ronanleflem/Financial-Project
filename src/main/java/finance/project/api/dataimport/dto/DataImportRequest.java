package finance.project.api.dataimport.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record DataImportRequest(
        @NotBlank String broker,
        @NotBlank String symbol,
        @NotBlank String timeframe,
        @NotNull Instant startDate,
        @NotNull Instant endDate,
        @NotBlank String sourceType,
        String venue,
        String timezone,
        String conflictPolicy,
        String rollover
) {

    @AssertTrue(message = "startDate must be before endDate")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return startDate.isBefore(endDate);
    }
}
