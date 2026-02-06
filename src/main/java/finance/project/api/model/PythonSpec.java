package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PythonSpec(
        String specType,
        Map<String, Object> payload
) {
}
