package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record SeasonalityProfile(
        String id,
        String measure,
        Integer retHorizon,
        Integer minSamplesBin,
        Map<String, Object> params
) {
}
