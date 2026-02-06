package finance.project.api.model.run;

import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.RunType;
import finance.project.api.model.run.SeasonalityBlock;
import finance.project.api.model.run.SeasonalityDataBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunRequestInputSeasonalityTest {

    @Test
    void roundTrip() throws Exception {
        String json = RunRequestInputTestSupport.readFixture("seasonality.json");
        RunRequestInput dto = RunRequestInputTestSupport.mapper().readValue(json, RunRequestInput.class);

        assertEquals("seasonality", dto.specType());
        assertEquals("2026-02-02", dto.catalogVersion());
        assertEquals(RunType.SEASONALITY, dto.runType());
        assertTrue(dto.data() instanceof SeasonalityDataBlock);
        assertNotNull(dto.seasonality());
        assertTrue(dto.seasonality() instanceof SeasonalityBlock);
        assertNotNull(dto.performance());

        SeasonalityDataBlock data = (SeasonalityDataBlock) dto.data();
        assertEquals("SPY", data.symbol());
        assertEquals(Integer.valueOf(2008), data.startYear());

        RunRequestInput roundTrip = RunRequestInputTestSupport.mapper()
                .readValue(RunRequestInputTestSupport.mapper().writeValueAsString(dto), RunRequestInput.class);
        assertEquals(RunType.SEASONALITY, roundTrip.runType());
    }
}
