package finance.project.api.universe;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.universe.api.UniverseController;
import finance.project.api.universe.service.UniverseService;
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

@WebMvcTest(UniverseController.class)
@TestPropertySource(properties = {
        "server.error.include-message=always",
        "server.error.include-binding-errors=always"
})
@ActiveProfiles("test")
class UniverseControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UniverseService universeService;

    @Test
    void shouldRejectBlankAndNullFields() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", "");
        payload.put("broker", "");
        payload.put("timeframe", "");
        payload.put("startDate", null);
        payload.put("endDate", null);

        mockMvc.perform(post("/api/universes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='code')].defaultMessage", hasItem("must not be blank")))
                .andExpect(jsonPath("$.errors[?(@.field=='broker')].defaultMessage", hasItem("must not be blank")))
                .andExpect(jsonPath("$.errors[?(@.field=='timeframe')].defaultMessage", hasItem("must not be blank")))
                .andExpect(jsonPath("$.errors[?(@.field=='startDate')].defaultMessage", hasItem("must not be null")))
                .andExpect(jsonPath("$.errors[?(@.field=='endDate')].defaultMessage", hasItem("must not be null")));
    }
}
