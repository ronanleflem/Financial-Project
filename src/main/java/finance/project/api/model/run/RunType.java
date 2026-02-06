package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RunType {
    DCA("dca"),
    BACKTEST("backtest"),
    MARKET_STATS("market_stats"),
    SEASONALITY("seasonality"),
    STRESS_TESTS("stress_tests");

    private final String value;

    RunType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RunType fromValue(String value) {
        for (RunType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown runType: " + value);
    }
}
