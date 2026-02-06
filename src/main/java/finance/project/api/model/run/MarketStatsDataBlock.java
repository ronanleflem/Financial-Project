package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record MarketStatsDataBlock(
        @NotBlank String symbol,
        @NotBlank String timeframe,
        @NotNull Integer lookback,
        @NotBlank String statsPack,
        @NotBlank String session,
        @NotNull Boolean includeWeekends
) implements DataBlock {
}
