package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PythonSpec(
        @JsonProperty("spec_type") String specType,
        @JsonIgnore Map<String, Object> payload
) {
    @JsonAnyGetter
    public Map<String, Object> getPayload() {
        return payload;
    }
}
