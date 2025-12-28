package finance.project.api.dataimport;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.dataimport.api.DataImportJobController;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DataImportJobController.class)
@TestPropertySource(properties = {
        "server.error.include-message=always",
        "server.error.include-binding-errors=always"
})
@ActiveProfiles("test")
class DataImportJobControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataImportService dataImportService;

    @Test
    void shouldRejectBlankFields() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("broker", "");
        payload.put("symbol", "");
        payload.put("timeframe", "");
        payload.put("startDate", Instant.parse("2024-01-01T00:00:00Z"));
        payload.put("endDate", Instant.parse("2024-01-02T00:00:00Z"));
        payload.put("sourceType", "");

        mockMvc.perform(post("/api/data-import/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='broker')].defaultMessage", hasItem("must not be blank")))
                .andExpect(jsonPath("$.errors[?(@.field=='symbol')].defaultMessage", hasItem("must not be blank")))
                .andExpect(jsonPath("$.errors[?(@.field=='timeframe')].defaultMessage", hasItem("must not be blank")))
                .andExpect(jsonPath("$.errors[?(@.field=='sourceType')].defaultMessage", hasItem("must not be blank")));
    }

    @Test
    void shouldRejectInvertedDates() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("broker", "BrokerA");
        payload.put("symbol", "EURUSD");
        payload.put("timeframe", "M1");
        payload.put("startDate", Instant.parse("2024-01-03T00:00:00Z"));
        payload.put("endDate", Instant.parse("2024-01-02T00:00:00Z"));
        payload.put("sourceType", "API");

        mockMvc.perform(post("/api/data-import/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].defaultMessage", hasItem("startDate must be before endDate")));
    }
}
