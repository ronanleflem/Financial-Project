package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record RunRequestInput(
        @NotBlank String specType,
        @NotBlank String catalogVersion,
        String requestId,
        @NotNull RunType runType,
        @NotNull @Valid DataBlock data,
        @Valid StrategyBlock strategy,
        @Valid BacktestSignalBlock signal,
        @Valid MarketStatsBlock stats,
        @Valid SeasonalityBlock seasonality,
        @Valid FiltersBlock filters,
        @Valid PerformanceBlock performance,
        Map<String, Object> output,
        @Valid PersistenceSpec persistence
) {
}
