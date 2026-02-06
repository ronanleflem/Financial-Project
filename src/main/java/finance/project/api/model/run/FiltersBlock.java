package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record FiltersBlock(
        @Valid List<FilterSpec> filters,
        @Valid List<FilterRuleSpec> rules,
        @Valid RulesConfig rulesConfig
) {
}
