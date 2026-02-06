package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record DcaStrategyCore(
        @NotNull DcaStrategyType type,
        List<String> grid,
        @NotNull @Valid DcaParams params
) implements StrategyBlock {
}
