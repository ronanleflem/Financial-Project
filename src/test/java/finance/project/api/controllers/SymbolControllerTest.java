package finance.project.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.model.SymbolDTO;
import finance.project.api.services.SymbolService;
import finance.project.api.services.SymbolServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@WebMvcTest(SymbolController.class)
class SymbolControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SymbolService symbolService;

    SymbolServiceImpl symbolServiceImpl;

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;

    @BeforeEach
    void setUp() {
        symbolServiceImpl = new SymbolServiceImpl();
    }

    private SymbolDTO createTestSymbolDTO() {
        return SymbolDTO.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build();
    }

    @Test
    void testDeleteSymbol() throws Exception {
        SymbolDTO symbol = symbolServiceImpl.listAllSymbols().get(0);

        given(symbolService.deleteSymbol(any(UUID.class))).willReturn(true);

        mockMvc.perform(delete("/api/finance/symbols/{symbolId}", symbol.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(symbolService).deleteSymbol(uuidArgumentCaptor.capture());

        assertThat(symbol.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void testUpdateSymbol() throws Exception {
        SymbolDTO symbol = createTestSymbolDTO();

        given(symbolService.updateSymbol(any(UUID.class), any(SymbolDTO.class))).willReturn(symbol);

        mockMvc.perform(put("/api/finance/symbols/{symbolId}", symbol.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(symbol)))
                .andExpect(status().isOk());

        verify(symbolService).updateSymbol(any(UUID.class), any(SymbolDTO.class));
    }

    @Test
    void testCreateNewSymbol() throws Exception {
        SymbolDTO symbol = createTestSymbolDTO();

        given(symbolService.createSymbol(any(SymbolDTO.class))).willReturn(symbol);

        mockMvc.perform(post("/api/finance/symbols")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(symbol)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "/api/finance/symbols/" + symbol.getId().toString()));

        verify(symbolService).createSymbol(any(SymbolDTO.class));
    }

    @Test
    void testListSymbols() throws Exception {
        List<SymbolDTO> symbolList = List.of(
                createTestSymbolDTO(),
                SymbolDTO.builder().id(UUID.randomUUID()).symbol("GOOGL").build()
        );

        given(symbolService.listAllSymbols()).willReturn(symbolList);

        mockMvc.perform(get("/api/finance/symbols")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].symbol", is("AAPL")))
                .andExpect(jsonPath("$[1].symbol", is("GOOGL")));
    }

    @Test
    void getSymbolById() throws Exception {
        SymbolDTO symbol = createTestSymbolDTO();

        given(symbolService.getSymbolById(any(UUID.class))).willReturn(Optional.of(symbol));

        mockMvc.perform(get("/api/finance/symbols/{id}", symbol.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(symbol.getId().toString())))
                .andExpect(jsonPath("$.name", is(symbol.getName())))
                .andExpect(jsonPath("$.symbol", is("AAPL")))
                .andExpect(jsonPath("$.market", is("NASDAQ")));
    }

    @Test
    void getSymbolByIdNotFound() throws Exception {
        given(symbolService.getSymbolById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(get("/api/finance/symbols/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateInvalidSymbol() throws Exception {
        SymbolDTO invalidSymbolDTO = SymbolDTO.builder()
                .symbol("") // Invalid symbol
                .name("Invalid Company")
                .market("Invalid Market")
                .build();

        mockMvc.perform(post("/api/finance/symbols")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSymbolDTO)))
                .andExpect(status().isBadRequest());
    }
}