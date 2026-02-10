package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record StressScenarioSpec(
        String type,
        Double shockPct,
        Double volMultiplier,
        Double drawdownPct,
        Integer window,
        String index
) {
}
