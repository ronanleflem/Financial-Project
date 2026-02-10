package finance.project.api.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.builders.MarketStatsSpecBuilder;
import finance.project.api.validation.RunRequestValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketStatsSpecBuilderValidationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void rejectsLookbackUnder100() throws Exception {
        String json = """
                {
                  "specType": "market_stats",
                  "catalogVersion": "2026-02-02",
                  "runType": "market_stats",
                  "data": {
                    "symbol": "SPY",
                    "timeframe": "1d",
                    "lookback": 10,
                    "statsPack": "Volatility",
                    "session": "Full",
                    "includeWeekends": false
                  },
                  "stats": {
                    "event": { "id": "k_consecutive", "params": {} },
                    "condition": { "id": "htf_trend", "params": {} },
                    "target": { "id": "continuation_n", "params": {} }
                  }
                }
                """;

        RunRequestInput input = MAPPER.readValue(json, RunRequestInput.class);
        RunRequestValidationException ex = assertThrows(
                RunRequestValidationException.class,
                () -> new MarketStatsSpecBuilder().build(input)
        );
        assertTrue(ex.getErrors().stream().anyMatch(it -> "data.lookback".equals(it.field())));
    }

    @Test
    void rejectsInvalidValidationNumbers() throws Exception {
        String json = """
                {
                  "specType": "market_stats",
                  "catalogVersion": "2026-02-02",
                  "runType": "market_stats",
                  "data": {
                    "symbol": "SPY",
                    "timeframe": "1d",
                    "lookback": 1200,
                    "statsPack": "Liquidity",
                    "session": "RTH",
                    "includeWeekends": true
                  },
                  "stats": {
                    "event": { "id": "gap_up", "params": {} },
                    "condition": { "id": "session", "params": {} },
                    "target": { "id": "retracement_probability", "params": {} },
                    "validation": {
                      "trainMonths": 0,
                      "testMonths": 1,
                      "folds": 1,
                      "embargoDays": 0
                    }
                  }
                }
                """;

        RunRequestInput input = MAPPER.readValue(json, RunRequestInput.class);
        RunRequestValidationException ex = assertThrows(
                RunRequestValidationException.class,
                () -> new MarketStatsSpecBuilder().build(input)
        );
        assertTrue(ex.getErrors().stream().anyMatch(it -> "stats.validation.trainMonths".equals(it.field())));
    }
}

