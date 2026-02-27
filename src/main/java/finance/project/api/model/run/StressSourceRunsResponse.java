package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "StressSourceRunsResponse")
public record StressSourceRunsResponse(
        List<StressSourceRunItem> items,
        @JsonProperty("next_cursor")
        @Schema(name = "next_cursor", description = "Cursor for the next page; omitted when no more results")
        String nextCursor
) {
}
