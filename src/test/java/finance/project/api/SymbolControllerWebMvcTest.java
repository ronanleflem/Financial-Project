package finance.project.api;

import finance.project.api.config.ValidationErrorHandler;
import finance.project.api.controllers.SymbolController;
import finance.project.api.services.SymbolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SymbolController.class)
@Import(ValidationErrorHandler.class)
@ActiveProfiles("test")
class SymbolControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SymbolService symbolService;

    @Test
    void shouldReturnBadRequestWithValidationErrors() throws Exception {
        mockMvc.perform(post("/api/finance/symbols")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("symbol", "name", "market")))
                .andExpect(jsonPath("$.errors[*].defaultMessage", everyItem(is("must not be null"))));
    }

    @Test
    void shouldReturnNotFoundWhenSymbolMissing() throws Exception {
        UUID symbolId = UUID.randomUUID();
        given(symbolService.getSymbolById(symbolId)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/finance/symbols/{symbolId}", symbolId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Value Not Found"))
                .andExpect(jsonPath("$.path").value("/api/finance/symbols/" + symbolId));
    }
}
