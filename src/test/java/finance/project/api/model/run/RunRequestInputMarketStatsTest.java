package finance.project.api.model.run;

import finance.project.api.model.run.MarketStatsBlock;
import finance.project.api.model.run.MarketStatsDataBlock;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.RunType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunRequestInputMarketStatsTest {

    @Test
    void roundTrip() throws Exception {
        String json = RunRequestInputTestSupport.readFixture("market-stats.json");
        RunRequestInput dto = RunRequestInputTestSupport.mapper().readValue(json, RunRequestInput.class);

        assertEquals("market_stats", dto.specType());
        assertEquals("2026-02-02", dto.catalogVersion());
        assertEquals(RunType.MARKET_STATS, dto.runType());
        assertTrue(dto.data() instanceof MarketStatsDataBlock);
        assertNotNull(dto.stats());
        assertTrue(dto.stats() instanceof MarketStatsBlock);

        MarketStatsDataBlock data = (MarketStatsDataBlock) dto.data();
        assertEquals("SPY", data.symbol());
        assertEquals(Integer.valueOf(1500), data.lookback());

        RunRequestInput roundTrip = RunRequestInputTestSupport.mapper()
                .readValue(RunRequestInputTestSupport.mapper().writeValueAsString(dto), RunRequestInput.class);
        assertEquals(RunType.MARKET_STATS, roundTrip.runType());
    }
}
