package finance.project.api.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.builders.SeasonalitySpecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeasonalitySpecBuilderGoldenTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void buildsMinimalSpecWithDefaults() throws Exception {
        assertGolden("seasonality-minimal-input.json", "seasonality-minimal-output.json");
    }

    @Test
    void buildsFullSpec() throws Exception {
        assertGolden("seasonality-full-input.json", "seasonality-full-output.json");
    }

    private void assertGolden(String inputFixture, String expectedFixture) throws Exception {
        RunRequestInput input = MAPPER.readValue(readFixture(inputFixture), RunRequestInput.class);
        PythonSpec spec = new SeasonalitySpecBuilder().build(input);

        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(spec));
        JsonNode expected = MAPPER.readTree(readFixture(expectedFixture));

        assertEquals(expected, actual);
    }

    private static String readFixture(String name) throws IOException {
        String path = "fixtures/spec-builder/" + name;
        try (InputStream stream = SeasonalitySpecBuilderGoldenTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

