package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record SeasonalityBlock(
        @Valid SeasonalityProfile profile,
        @Valid SeasonalitySignal signal,
        @Valid SeasonalityCompute compute,
        @Valid SeasonalityExecution execution,
        Map<String, Object> risk,
        Map<String, Object> tpSl,
        @Valid ValidationSpec validation,
        @Valid PersistenceSpec persistence,
        @Valid ArtifactsSpec artifacts
) {
}
