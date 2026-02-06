package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DcaEquityParams.class, name = "dca_equity"),
        @JsonSubTypes.Type(value = DcaEtfParams.class, name = "dca_etf"),
        @JsonSubTypes.Type(value = CryptoGridParams.class, name = "crypto_grid")
})
@JsonIgnoreProperties(ignoreUnknown = false)
public interface DcaParams {
}
