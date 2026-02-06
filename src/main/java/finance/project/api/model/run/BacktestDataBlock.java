package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record BacktestDataBlock(
        @NotBlank String symbol,
        @NotBlank String timeframe,
        @NotBlank String startDate,
        @NotBlank String endDate,
        @NotBlank String strategyName
) implements DataBlock {
}
