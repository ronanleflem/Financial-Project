package finance.project.api.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.BacktestDataBlock;
import finance.project.api.model.run.DcaDataBlock;
import finance.project.api.model.run.DcaEquityParams;
import finance.project.api.model.run.DcaStrategyCore;
import finance.project.api.model.run.DcaStrategyType;
import finance.project.api.model.run.MarketStatsDataBlock;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.RunType;
import finance.project.api.model.run.SeasonalityDataBlock;
import finance.project.api.model.run.StressTestsDataBlock;
import finance.project.api.spec.builders.BacktestSpecBuilder;
import finance.project.api.spec.builders.MarketStatsSpecBuilder;
import finance.project.api.spec.builders.SeasonalitySpecBuilder;
import finance.project.api.spec.builders.StrategyBacktestSpecBuilder;
import finance.project.api.spec.builders.StressTestsSpecBuilder;
import org.junit.jupiter.api.Test;

class SpecBuilderRegistrationTest {

    @Test
    void backtestBuilderBuildsSpec() {
        BacktestSpecBuilder builder = new BacktestSpecBuilder();
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
                null,
                null,
                null
        );

        PythonSpec spec = builder.build(input);

        assertEquals("backtest", builder.supportsSpecType());
        assertEquals("backtest", spec.specType());
    }

    @Test
    void dcaBuilderBuildsSpec() {
        StrategyBacktestSpecBuilder builder = new StrategyBacktestSpecBuilder();
        RunRequestInput input = new RunRequestInput(
                "dca",
                "2026-02-02",
                null,
                RunType.DCA,
                new DcaDataBlock("BTCUSD", "1d", "weekly", 250, "2022-01-01", "2023-01-01"),
                new DcaStrategyCore(
                        DcaStrategyType.DCA_EQUITY,
                        java.util.List.of("grid_conservative"),
                        new DcaEquityParams("rolling_high", "limit", "default", true)
                ),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        PythonSpec spec = builder.build(input);

        assertEquals("dca", builder.supportsSpecType());
        assertEquals("dca", spec.specType());
    }

    @Test
    void marketStatsBuilderBuildsSpec() {
        MarketStatsSpecBuilder builder = new MarketStatsSpecBuilder();
        RunRequestInput input = new RunRequestInput(
                "market_stats",
                "2026-02-02",
                null,
                RunType.MARKET_STATS,
                new MarketStatsDataBlock("SPY", "1d", 1500, "Volatility", "Full", false),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        PythonSpec spec = builder.build(input);

        assertEquals("market_stats", builder.supportsSpecType());
        assertEquals("market_stats", spec.specType());
    }

    @Test
    void seasonalityBuilderBuildsSpec() {
        SeasonalitySpecBuilder builder = new SeasonalitySpecBuilder();
        RunRequestInput input = new RunRequestInput(
                "seasonality",
                "2026-02-02",
                null,
                RunType.SEASONALITY,
                new SeasonalityDataBlock("SPY", "1d", "Monthly", 2008, 2024, "All", true),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        PythonSpec spec = builder.build(input);

        assertEquals("seasonality", builder.supportsSpecType());
        assertEquals("seasonality", spec.specType());
    }

    @Test
    void stressTestsBuilderBuildsSpec() {
        StressTestsSpecBuilder builder = new StressTestsSpecBuilder();
        RunRequestInput input = new RunRequestInput(
                "stress_tests",
                "2026-02-02",
                null,
                RunType.STRESS_TESTS,
                new StressTestsDataBlock("SPY", "1d"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        PythonSpec spec = builder.build(input);

        assertEquals("stress_tests", builder.supportsSpecType());
        assertEquals("stress_tests", spec.specType());
    }
}
