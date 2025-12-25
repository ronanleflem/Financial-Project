package finance.project.api.universe.api;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.Symbol;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.universe.Universe;
import finance.project.api.universe.UniverseRepository;
import finance.project.api.universe.UniverseType;
import finance.project.api.universe.client.CoingeckoUniverseClient;
import finance.project.api.universe.dto.UniverseImportRequest;
import finance.project.api.universe.service.UniverseImportRunner;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UniverseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UniverseRepository universeRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @MockBean
    private CoingeckoUniverseClient coingeckoUniverseClient;

    @MockBean
    private UniverseImportRunner universeImportRunner;

    @BeforeEach
    void setUp() {
        universeRepository.deleteAll();
        symbolRepository.deleteAll();
        when(coingeckoUniverseClient.listCategories()).thenReturn(List.of());

        Symbol symbol = symbolRepository.save(Symbol.builder()
                .symbol("AAPL")
                .name("Apple")
                .market("EQUITY")
                .build());

        Universe universe = Universe.builder()
                .code("TECH")
                .name("Tech Universe")
                .type(UniverseType.EQUITY)
                .provider("CSV_MANUAL")
                .build();
        universe.addSymbol(symbol);
        symbol.getUniverses().add(universe);
        universeRepository.save(universe);
    }

    @Test
    void getCatalogReturnsNonEmptyList() throws Exception {
        mockMvc.perform(get("/api/universes/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void importUniverseWithValidDatesReturnsAccepted() throws Exception {
        UniverseImportRequest request = new UniverseImportRequest(
                "AAPLMSFT",
                "BINANCE",
                "1d",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-02-01T00:00:00Z"),
                null,
                null
        );

        doNothing().when(universeImportRunner).runUniverseImport(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/universes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.universeId").isNumber())
                .andExpect(jsonPath("$.code").value("AAPLMSFT"))
                .andExpect(jsonPath("$.type").value("EQUITY"))
                .andExpect(jsonPath("$.provider").value("CSV_MANUAL"));

        verify(universeImportRunner).runUniverseImport(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void importUniverseWithInvalidDatesReturnsBadRequest() throws Exception {
        UniverseImportRequest request = new UniverseImportRequest(
                "AAPLMSFT",
                "BINANCE",
                "1d",
                Instant.parse("2024-02-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"),
                null,
                null
        );

        mockMvc.perform(post("/api/universes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUniverseReturnsOkWhenExists() throws Exception {
        mockMvc.perform(get("/api/universes/TECH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TECH"))
                .andExpect(jsonPath("$.symbols").isArray())
                .andExpect(jsonPath("$.symbols[0]").value("AAPL"));
    }

    @Test
    void getUniverseReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/universes/UNKNOWN"))
                .andExpect(status().isNotFound());
    }
}
