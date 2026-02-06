package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record SeasonalityDataBlock(
        @NotBlank String symbol,
        @NotBlank String timeframe,
        @NotBlank String window,
        @NotNull Integer startYear,
        @NotNull Integer endYear,
        @NotBlank String filter,
        @NotNull Boolean normalize
) implements DataBlock {
}
