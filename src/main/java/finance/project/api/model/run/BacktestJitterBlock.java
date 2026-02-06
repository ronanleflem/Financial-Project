package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record BacktestJitterBlock(
        Boolean enabled,
        String dist,
        Integer tpBps,
        Integer slBps,
        Integer seed
) {
}
