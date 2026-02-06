package finance.project.api.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.builders.BacktestSpecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BacktestSpecBuilderGoldenTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);

    @Test
    void buildsMinimalBacktestSpec() throws Exception {
        assertGolden("backtest-minimal-input.json", "backtest-minimal-output.json");
    }

    @Test
    void buildsFullBacktestSpec() throws Exception {
        assertGolden("backtest-full-input.json", "backtest-full-output.json");
    }

    private void assertGolden(String inputFixture, String expectedFixture) throws Exception {
        RunRequestInput input = MAPPER.readValue(readFixture(inputFixture), RunRequestInput.class);
        PythonSpec spec = new BacktestSpecBuilder().build(input);

        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(spec));
        JsonNode expected = MAPPER.readTree(readFixture(expectedFixture));

        assertEquals(expected, actual);
    }

    private static String readFixture(String name) throws IOException {
        String path = "fixtures/spec-builder/" + name;
        try (InputStream stream = BacktestSpecBuilderGoldenTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
