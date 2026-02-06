package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record MarketStatsBlock(
        @NotNull @Valid MarketEventSpec event,
        @NotNull @Valid MarketConditionSpec condition,
        @NotNull @Valid MarketTargetSpec target,
        @Valid ValidationSpec validation,
        @Valid PersistenceSpec persistence,
        @Valid ArtifactsSpec artifacts
) {
}
