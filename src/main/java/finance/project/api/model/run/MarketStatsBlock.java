package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketStatsBlock(
        @Valid MarketEventSpec event,
        @Valid MarketConditionSpec condition,
        @Valid MarketTargetSpec target,
        @Valid ValidationSpec validation,
        @Valid PersistenceSpec persistence,
        @Valid ArtifactsSpec artifacts
) {
}
