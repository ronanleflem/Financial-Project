package finance.project.api.universe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UniverseImportRequest(
        @NotBlank String code,
        @NotBlank String broker,
        @NotBlank String timeframe,
        @NotNull Instant startDate,
        @NotNull Instant endDate,
        String venue,
        String assetClass
) {
}
