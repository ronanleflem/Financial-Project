package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record MonteCarloStressTests(
        Boolean enabled,
        Integer nSims,
        Integer seed,
        String method,
        Integer blockSize,
        Boolean overlapping,
        @Valid TimeDistributionSpec timeDistribution,
        @Valid ParamDriftSpec paramDrift,
        @Valid SizingSpec sizing,
        @Valid StressOutputSpec output,
        @Valid List<StressScenarioSpec> scenarios,
        @Valid MultiAssetSpec multiAsset
) {
}
