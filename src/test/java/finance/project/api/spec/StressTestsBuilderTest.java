package finance.project.api.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.model.run.MonteCarloStressTests;
import finance.project.api.spec.builders.StressTestsBuilder;
import finance.project.api.validation.RunRequestValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StressTestsBuilderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void buildsMinimalGolden() throws Exception {
        assertGolden("stress-tests-minimal-input.json", "stress-tests-minimal-output.json");
    }

    @Test
    void buildsFullGolden() throws Exception {
        assertGolden("stress-tests-full-input.json", "stress-tests-full-output.json");
    }

    @Test
    void rejectsNonPositiveNSims() throws Exception {
        MonteCarloStressTests input = MAPPER.readValue(readFixture("stress-tests-invalid-input.json"), MonteCarloStressTests.class);
        RunRequestValidationException ex = assertThrows(
                RunRequestValidationException.class,
                () -> new StressTestsBuilder().build(input)
        );
        assertTrue(ex.getErrors().stream().anyMatch(it -> "performance.stressTests.nSims".equals(it.field())));
    }

    private void assertGolden(String inputFixture, String expectedFixture) throws Exception {
        MonteCarloStressTests input = MAPPER.readValue(readFixture(inputFixture), MonteCarloStressTests.class);
        Map<String, Object> spec = new StressTestsBuilder().build(input);

        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(spec));
        JsonNode expected = MAPPER.readTree(readFixture(expectedFixture));

        assertEquals(expected, actual);
    }

    private static String readFixture(String name) throws IOException {
        String path = "fixtures/stress-tests-builder/" + name;
        try (InputStream stream = StressTestsBuilderTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

