package finance.project.api.model.run;

import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.RunType;
import finance.project.api.model.run.StressTestsDataBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunRequestInputStressTestsTest {

    @Test
    void roundTrip() throws Exception {
        String json = RunRequestInputTestSupport.readFixture("stress-tests.json");
        RunRequestInput dto = RunRequestInputTestSupport.mapper().readValue(json, RunRequestInput.class);

        assertEquals("stress_tests", dto.specType());
        assertEquals("2026-02-02", dto.catalogVersion());
        assertEquals(RunType.STRESS_TESTS, dto.runType());
        assertTrue(dto.data() instanceof StressTestsDataBlock);
        assertNotNull(dto.performance());
        assertNotNull(dto.performance().stressTests());

        StressTestsDataBlock data = (StressTestsDataBlock) dto.data();
        assertEquals("SPY", data.symbol());
        assertEquals("1d", data.timeframe());

        RunRequestInput roundTrip = RunRequestInputTestSupport.mapper()
                .readValue(RunRequestInputTestSupport.mapper().writeValueAsString(dto), RunRequestInput.class);
        assertEquals(RunType.STRESS_TESTS, roundTrip.runType());
    }
}
