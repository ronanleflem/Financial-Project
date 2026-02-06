package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "runType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DcaDataBlock.class, name = "dca"),
        @JsonSubTypes.Type(value = BacktestDataBlock.class, name = "backtest"),
        @JsonSubTypes.Type(value = MarketStatsDataBlock.class, name = "market_stats"),
        @JsonSubTypes.Type(value = SeasonalityDataBlock.class, name = "seasonality"),
        @JsonSubTypes.Type(value = StressTestsDataBlock.class, name = "stress_tests")
})
@JsonIgnoreProperties(ignoreUnknown = false)
public interface DataBlock {
}
