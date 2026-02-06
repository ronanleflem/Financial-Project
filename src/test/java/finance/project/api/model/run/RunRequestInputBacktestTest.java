package finance.project.api.model.run;

import finance.project.api.model.run.BacktestDataBlock;
import finance.project.api.model.run.BacktestStrategyBlock;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.RunType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunRequestInputBacktestTest {

    @Test
    void roundTrip() throws Exception {
        String json = RunRequestInputTestSupport.readFixture("backtest.json");
        RunRequestInput dto = RunRequestInputTestSupport.mapper().readValue(json, RunRequestInput.class);

        assertEquals("backtest", dto.specType());
        assertEquals("2026-02-02", dto.catalogVersion());
        assertEquals(RunType.BACKTEST, dto.runType());
        assertTrue(dto.data() instanceof BacktestDataBlock);
        assertTrue(dto.strategy() instanceof BacktestStrategyBlock);
        assertNotNull(dto.signal());
        assertNotNull(dto.filters());
        assertNotNull(dto.performance());

        BacktestDataBlock data = (BacktestDataBlock) dto.data();
        assertEquals("SPY", data.symbol());
        assertEquals("1d", data.timeframe());
        assertEquals("Breakout", data.strategyName());

        BacktestStrategyBlock strategy = (BacktestStrategyBlock) dto.strategy();
        assertEquals("Breakout", strategy.name());
        assertEquals("atr_trailing", strategy.tpSl().dynamicSl().mode());
        assertEquals(Boolean.FALSE, strategy.tpSl().jitter().enabled());

        RunRequestInput roundTrip = RunRequestInputTestSupport.mapper()
                .readValue(RunRequestInputTestSupport.mapper().writeValueAsString(dto), RunRequestInput.class);
        assertEquals(RunType.BACKTEST, roundTrip.runType());
        assertTrue(roundTrip.data() instanceof BacktestDataBlock);
    }
}
