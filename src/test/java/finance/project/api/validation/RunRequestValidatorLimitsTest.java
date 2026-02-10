package finance.project.api.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import finance.project.api.model.run.BacktestDataBlock;
import finance.project.api.model.run.MonteCarloStressTests;
import finance.project.api.model.run.PerformanceBlock;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.RunType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RunRequestValidatorLimitsTest {

    @Test
    void rejectsTooManySims() {
        RunLimitsProperties limits = new RunLimitsProperties();
        limits.setMaxStressTestSims(5000);
        RunRequestValidator validator = new RunRequestValidator(Optional.of(limits));

        MonteCarloStressTests stressTests = new MonteCarloStressTests(
                true,
                6000,
                1,
                "monte_carlo",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        PerformanceBlock performance = new PerformanceBlock(
                10_000.0,
                null,
                null,
                null,
                null,
                stressTests
        );

        RunRequestInput input = new RunRequestInput(
                "backtest",
                "2026-02-02",
                null,
                RunType.BACKTEST,
                new BacktestDataBlock("SPY", "1d", "2022-01-01", "2023-01-01", "Breakout"),
                null,
                null,
                null,
                null,
                null,
                performance,
                null,
                null
        );

        var errors = validator.validate(input);
        assertTrue(errors.stream().anyMatch(e -> "performance.stressTests.nSims".equals(e.field())));
    }

    @Test
    void rejectsTooWideDateRange() {
        RunLimitsProperties limits = new RunLimitsProperties();
        limits.setMaxDateRangeDays(10);
        RunRequestValidator validator = new RunRequestValidator(Optional.of(limits));

        RunRequestInput input = new RunRequestInput(
                "backtest",
                "2026-02-02",
                null,
                RunType.BACKTEST,
                new BacktestDataBlock("SPY", "1d", "2022-01-01", "2022-02-01", "Breakout"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var errors = validator.validate(input);
        assertTrue(errors.stream().anyMatch(e -> "data.endDate".equals(e.field())));
    }
}

