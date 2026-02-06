package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record PerformanceBlock(
        Double initialCapital,
        Double capitalPerUnit,
        Double maxCapitalPerTrade,
        Double riskPct,
        Double riskFreeRatePct,
        @Valid MonteCarloStressTests stressTests
) {
}
