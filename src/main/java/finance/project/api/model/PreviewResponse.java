package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreviewResponse(
        PythonSpec spec,
        List<String> warnings,
        List<String> normalizedFields
) {
}

