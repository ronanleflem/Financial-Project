package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record SeasonalityBlock(
        @Valid SeasonalityProfile profile,
        @Valid SeasonalitySignal signal,
        @Valid SeasonalityCompute compute,
        @Valid SeasonalityExecution execution,
        @Valid ValidationSpec validation,
        @Valid PersistenceSpec persistence,
        @Valid ArtifactsSpec artifacts
) {
}
