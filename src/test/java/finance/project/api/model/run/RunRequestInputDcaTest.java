package finance.project.api.model.run;

import finance.project.api.model.run.DcaDataBlock;
import finance.project.api.model.run.DcaStrategyCore;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.RunType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunRequestInputDcaTest {

    @Test
    void roundTrip() throws Exception {
        String json = RunRequestInputTestSupport.readFixture("dca.json");
        RunRequestInput dto = RunRequestInputTestSupport.mapper().readValue(json, RunRequestInput.class);

        assertEquals("dca", dto.specType());
        assertEquals("2026-02-02", dto.catalogVersion());
        assertEquals("req-dca-1", dto.requestId());
        assertEquals(RunType.DCA, dto.runType());
        assertTrue(dto.data() instanceof DcaDataBlock);
        assertTrue(dto.strategy() instanceof DcaStrategyCore);
        assertNotNull(dto.filters());
        assertNotNull(dto.performance());

        DcaDataBlock data = (DcaDataBlock) dto.data();
        assertEquals("BTCUSD", data.symbol());
        assertEquals(Integer.valueOf(250), data.amount());

        RunRequestInput roundTrip = RunRequestInputTestSupport.mapper()
                .readValue(RunRequestInputTestSupport.mapper().writeValueAsString(dto), RunRequestInput.class);
        assertEquals(RunType.DCA, roundTrip.runType());
    }
}
