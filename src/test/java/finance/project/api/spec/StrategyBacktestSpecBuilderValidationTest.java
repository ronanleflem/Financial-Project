package finance.project.api.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.builders.StrategyBacktestSpecBuilder;
import finance.project.api.validation.RunRequestValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyBacktestSpecBuilderValidationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void throwsWhenSpecTypeIsNotDca() throws Exception {
        String json = """
                {
                  "specType": "backtest",
                  "catalogVersion": "2026-02-02",
                  "runType": "dca",
                  "data": {
                    "symbol": "BTCUSD",
                    "timeframe": "1d",
                    "frequency": "weekly",
                    "amount": 250,
                    "startDate": "2022-01-01",
                    "endDate": "2025-01-01"
                  },
                  "strategy": {
                    "type": "dca_equity",
                    "grid": ["grid_balanced"],
                    "params": {
                      "drawdownReference": "rolling_high",
                      "executionMode": "limit",
                      "tpSlPreset": "tp_2_sl_1"
                    }
                  }
                }
                """;

        RunRequestInput input = MAPPER.readValue(json, RunRequestInput.class);
        assertThrows(InvalidSpecTypeException.class, () -> new StrategyBacktestSpecBuilder().build(input));
    }

    @Test
    void throwsWhenGridIsEmpty() throws Exception {
        String json = """
                {
                  "specType": "dca",
                  "catalogVersion": "2026-02-02",
                  "runType": "dca",
                  "data": {
                    "symbol": "BTCUSD",
                    "timeframe": "1d",
                    "frequency": "weekly",
                    "amount": 250,
                    "startDate": "2022-01-01",
                    "endDate": "2025-01-01"
                  },
                  "strategy": {
                    "type": "dca_equity",
                    "grid": [],
                    "params": {
                      "drawdownReference": "rolling_high",
                      "executionMode": "limit",
                      "tpSlPreset": "tp_2_sl_1"
                    }
                  }
                }
                """;

        RunRequestInput input = MAPPER.readValue(json, RunRequestInput.class);
        RunRequestValidationException ex = assertThrows(
                RunRequestValidationException.class,
                () -> new StrategyBacktestSpecBuilder().build(input)
        );

        assertTrue(ex.getErrors().stream().anyMatch(it -> "strategy.grid".equals(it.field())));
    }

    @Test
    void throwsWhenTpSlIsMissingForEquity() throws Exception {
        String json = """
                {
                  "specType": "dca",
                  "catalogVersion": "2026-02-02",
                  "runType": "dca",
                  "data": {
                    "symbol": "BTCUSD",
                    "timeframe": "1d",
                    "frequency": "weekly",
                    "amount": 250,
                    "startDate": "2022-01-01",
                    "endDate": "2025-01-01"
                  },
                  "strategy": {
                    "type": "dca_equity",
                    "grid": ["grid_balanced"],
                    "params": {
                      "drawdownReference": "rolling_high",
                      "executionMode": "limit"
                    }
                  }
                }
                """;

        RunRequestInput input = MAPPER.readValue(json, RunRequestInput.class);
        RunRequestValidationException ex = assertThrows(
                RunRequestValidationException.class,
                () -> new StrategyBacktestSpecBuilder().build(input)
        );

        assertTrue(ex.getErrors().stream().anyMatch(it -> "strategy.params.tp_sl".equals(it.field())));
    }
}

