package finance.project.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.bootstrap.BootstrapData;
import finance.project.api.controllers.ComparisonController;
import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.model.ComparisonRequestDTO;
import finance.project.api.model.PerformanceComparisonDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.ComparisonRepository;
import finance.project.api.repositories.SymbolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComparisonController.class)
@ExtendWith(SpringExtension.class)
@Import(ComServiceTestConfig.class)
public class ComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CandleRepository candleRepository;

    @MockBean
    private SymbolRepository symbolRepository;

    @MockBean
    private ComparisonRepository comparisonRepository;

    @Autowired
    private ObjectMapper objectMapper;


    private ComparisonRequestDTO createTestComparisonRequest() {
        return ComparisonRequestDTO.builder()
                .symbol1("AAPL")
                .symbol2("GOOGL")
                .startDate(LocalDate.of(2023, 1, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                //.startDate(LocalDate.now())
                //.endDate(LocalDate.now())
                .build();
    }

    private PerformanceComparisonDTO createTestPerformanceComparisonDTO() {
        return PerformanceComparisonDTO.builder()
                .symbol1("AAPL")
                .performance1(BigDecimal.valueOf(10.0))
                .symbol2("GOOGL")
                .performance2(BigDecimal.valueOf(15.0))
                .build();
    }

    @Test
    void testCompareSymbols() throws Exception {
        ComparisonRequestDTO request = createTestComparisonRequest();
        //PerformanceComparisonDTO response = createTestPerformanceComparisonDTO();

        //SymbolDTO symbolDTO = new SymbolDTO(UUID.randomUUID(),request.getSymbol1(),"nameS", "marketS");
        //Optional<Symbol> symbol = symbolRepository.findById(symbolDTO.getId());
        Symbol symbol1 = new Symbol(UUID.randomUUID(),request.getSymbol1(),"Apple Inc.", "NASDAQ",List.of());
        Symbol symbol2 = new Symbol(UUID.randomUUID(),request.getSymbol2(),"Google LLC", "NASDAQ",List.of());

        Candle candleAAPLStart = new Candle(UUID.randomUUID(), symbol1, LocalDate.of(2023, 1, 1),
                BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(105.00), BigDecimal.valueOf(95.00),
                BigDecimal.valueOf(1000000));

        Candle candleAAPLEnd = new Candle(UUID.randomUUID(), symbol1, LocalDate.of(2023, 12, 31),
                BigDecimal.valueOf(110.00), BigDecimal.valueOf(110.00),
                BigDecimal.valueOf(115.00), BigDecimal.valueOf(105.00),
                BigDecimal.valueOf(1000000));

        Candle candleGOOGLStart = new Candle(UUID.randomUUID(), symbol2, LocalDate.of(2023, 1, 1),
                BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(105.00), BigDecimal.valueOf(95.00),
                BigDecimal.valueOf(1000000));

        Candle candleGOOGLEnd = new Candle(UUID.randomUUID(), symbol2, LocalDate.of(2023, 12, 31),
                BigDecimal.valueOf(115.00), BigDecimal.valueOf(115.00),
                BigDecimal.valueOf(120.00), BigDecimal.valueOf(110.00),
                BigDecimal.valueOf(1000000));

        given(symbolRepository.findBySymbol(request.getSymbol1())).willReturn(Optional.of(symbol1));
        given(candleRepository.findBySymbolAndDateBetween(symbol1,LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .willReturn(List.of(candleAAPLStart, candleAAPLEnd)); // Dangeureux car aléatoire dans le temps + prob d'implem

        given(symbolRepository.findBySymbol(request.getSymbol2())).willReturn(Optional.of(symbol2));
        given(candleRepository.findBySymbolAndDateBetween(symbol2,LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)))
                .willReturn(List.of(candleGOOGLStart, candleGOOGLEnd)); // Dangeureux car aléatoire dans le temps + prob d'implem


        mockMvc.perform(post("/api/finance/comparison")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)) // Ou new dépends des écoles
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.symbol1", is("AAPL")))
                .andExpect(jsonPath("$.performance1", is(10.0)))
                .andExpect(jsonPath("$.symbol2", is("GOOGL")))
                .andExpect(jsonPath("$.performance2", is(15.0)));
    }
    /**
    @Test
    void testCompareSymbolsNotFound() throws Exception {
        ComparisonRequestDTO request = createTestComparisonRequest();

        given(comparisonService.comparePerformance(
                anyString(), anyString(), any(LocalDate.class), any(LocalDate.class))
        ).willThrow(new NotFoundException("Symbol not found"));

        mockMvc.perform(post("/api/finance/comparison")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    */
}
