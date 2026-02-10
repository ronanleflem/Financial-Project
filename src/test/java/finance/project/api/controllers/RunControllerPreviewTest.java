package finance.project.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.config.RunValidationErrorHandler;
import finance.project.api.observability.RunMetrics;
import finance.project.api.services.PythonSpecService;
import finance.project.api.services.RunRequestService;
import finance.project.api.services.RunResultService;
import finance.project.api.services.RunStatusService;
import finance.project.api.spec.DefaultSpecBuilderFactory;
import finance.project.api.spec.builders.BacktestSpecBuilder;
import finance.project.api.spec.builders.SeasonalitySpecBuilder;
import finance.project.api.spec.builders.StressTestsBuilder;
import finance.project.api.validation.RunRequestValidator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest(RunController.class)
@Import({
        RunRequestValidator.class,
        RunValidationErrorHandler.class,
        PythonSpecService.class,
        DefaultSpecBuilderFactory.class,
        StressTestsBuilder.class,
        BacktestSpecBuilder.class,
        SeasonalitySpecBuilder.class
})
class RunControllerPreviewTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RunRequestService runRequestService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RunStatusService runStatusService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RunResultService runResultService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RunMetrics runMetrics;

    @Test
    void previewBacktestIsIdempotentAndHasNoNormalizedFields() throws Exception {
        String body = readFixture("fixtures/spec-builder/backtest-full-input.json");

        JsonNode first = callPreview(body);
        JsonNode second = callPreview(body);

        assertEquals(first, second);
        assertEquals(MAPPER.createArrayNode(), first.get("warnings"));
        assertEquals(MAPPER.createArrayNode(), first.get("normalizedFields"));
        assertEquals("backtest", first.get("spec").get("spec_type").asText());
    }

    @Test
    void previewSeasonalityMinimalReportsDefaultsAsNormalizedFields() throws Exception {
        String body = readFixture("fixtures/spec-builder/seasonality-minimal-input.json");
        JsonNode actual = callPreview(body);
        JsonNode expected = MAPPER.readTree(readFixture("fixtures/controller-preview/seasonality-minimal-response.json"));
        assertEquals(expected, actual);
    }

    private JsonNode callPreview(String jsonBody) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/specs/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static String readFixture(String path) throws IOException {
        try (InputStream stream = RunControllerPreviewTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
