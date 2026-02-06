package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record BacktestScreeningBlock(
        Boolean enabled,
        @Valid BacktestScreeningWindow window,
        Integer maxBars,
        Integer maxTrades,
        Integer maxSeconds
) {
}
