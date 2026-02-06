package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DcaStrategyType {
    DCA_EQUITY("dca_equity"),
    DCA_ETF("dca_etf"),
    CRYPTO_GRID("crypto_grid");

    private final String value;

    DcaStrategyType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DcaStrategyType fromValue(String value) {
        for (DcaStrategyType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown dca strategy type: " + value);
    }
}
