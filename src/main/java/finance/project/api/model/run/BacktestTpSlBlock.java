package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record BacktestTpSlBlock(
        Integer atrWindow,
        Double atrK,
        Double rMult,
        Integer slippageBps,
        Integer feeBps,
        Double stopLossPct,
        Double takeProfitPct,
        Boolean trailingStop,
        @Valid BacktestDynamicSlBlock dynamicSl,
        @Valid BacktestJitterBlock jitter
) {
}
