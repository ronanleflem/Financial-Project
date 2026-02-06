package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record BacktestStrategyBlock(
        @NotBlank String name,
        @Valid BacktestTpSlBlock tpSl,
        @Valid BacktestScreeningBlock screening
) implements StrategyBlock {
}
