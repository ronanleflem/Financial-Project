package finance.project.api.universe.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.universe.dto.UniverseCatalogDTO;
import finance.project.api.universe.dto.UniverseDetailsDTO;
import finance.project.api.universe.dto.UniverseImportRequest;
import finance.project.api.universe.dto.UniverseImportResponse;
import finance.project.api.universe.service.UniverseService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UniverseController.class)
@ActiveProfiles("test")
class UniverseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UniverseService universeService;

    @Test
    void getCatalogReturnsUniverses() throws Exception {
        List<UniverseCatalogDTO> catalog = List.of(
                new UniverseCatalogDTO("us-equities", "US Equities", "EQUITY", "IBKR", 1200, true),
                new UniverseCatalogDTO("crypto", "Crypto", "CRYPTO", "BINANCE", 200, false)
        );

        given(universeService.getCatalog()).willReturn(catalog);

        mockMvc.perform(get("/api/universes/catalog")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].code", is("us-equities")))
                .andExpect(jsonPath("$[0].importable", is(true)))
                .andExpect(jsonPath("$[1].provider", is("BINANCE")));
    }

    @Test
    void importUniverseAcceptsRequest() throws Exception {
        UniverseImportRequest request = new UniverseImportRequest(
                "us-equities",
                "IBKR",
                "1d",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-10T00:00:00Z"),
                "NYSE",
                "EQUITY"
        );

        UniverseImportResponse response = new UniverseImportResponse(42L, "us-equities", "EQUITY", "IBKR");

        given(universeService.importUniverse(request)).willReturn(response);

        mockMvc.perform(post("/api/universes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.universeId", is(42)))
                .andExpect(jsonPath("$.code", is("us-equities")))
                .andExpect(jsonPath("$.provider", is("IBKR")));
    }

    @Test
    void importUniverseValidatesPayload() throws Exception {
        UniverseImportRequest invalidRequest = new UniverseImportRequest(
                "",
                "",
                "",
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/universes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUniverseReturnsDetails() throws Exception {
        UniverseDetailsDTO details = new UniverseDetailsDTO(
                10L,
                "us-equities",
                "US Equities",
                "EQUITY",
                "IBKR",
                List.of("AAPL", "MSFT")
        );

        given(universeService.getUniverseDetails("us-equities")).willReturn(Optional.of(details));

        mockMvc.perform(get("/api/universes/us-equities")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code", is("us-equities")))
                .andExpect(jsonPath("$.symbols.length()", is(2)))
                .andExpect(jsonPath("$.symbols[0]", is("AAPL")));
    }

    @Test
    void getUniverseReturnsNotFound() throws Exception {
        given(universeService.getUniverseDetails("missing")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/universes/missing")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
