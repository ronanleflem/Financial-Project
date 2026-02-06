package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public record DcaDataBlock(
        @NotBlank String symbol,
        @NotBlank String timeframe,
        @NotBlank String frequency,
        @NotNull Integer amount,
        @NotBlank String startDate,
        @NotBlank String endDate
) implements DataBlock {
}
