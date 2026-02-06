package finance.project.api.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.builders.StrategyBacktestSpecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrategyBacktestSpecBuilderGoldenTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void buildsDcaEquitySpec() throws Exception {
        assertGolden("dca-equity-input.json", "dca-equity-output.json");
    }

    @Test
    void buildsDcaEtfSpec() throws Exception {
        assertGolden("dca-etf-input.json", "dca-etf-output.json");
    }

    @Test
    void buildsCryptoGridSpec() throws Exception {
        assertGolden("dca-crypto-grid-input.json", "dca-crypto-grid-output.json");
    }

    private void assertGolden(String inputFixture, String expectedFixture) throws Exception {
        RunRequestInput input = MAPPER.readValue(readFixture(inputFixture), RunRequestInput.class);
        PythonSpec spec = new StrategyBacktestSpecBuilder().build(input);

        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(spec));
        JsonNode expected = MAPPER.readTree(readFixture(expectedFixture));

        assertEquals(expected, actual);
    }

    private static String readFixture(String name) throws IOException {
        String path = "fixtures/spec-builder/" + name;
        try (InputStream stream = StrategyBacktestSpecBuilderGoldenTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

