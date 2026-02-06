package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "runType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DcaStrategyCore.class, name = "dca"),
        @JsonSubTypes.Type(value = BacktestStrategyBlock.class, name = "backtest")
})
@JsonIgnoreProperties(ignoreUnknown = false)
public interface StrategyBlock {
}
