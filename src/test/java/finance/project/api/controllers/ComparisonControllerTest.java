package finance.project.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.model.ComparisonRequestDTO;
import finance.project.api.model.PerformanceComparisonDTO;
import finance.project.api.services.ComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComparisonController.class)
public class ComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComparisonService comparisonService;

    @Autowired
    private ObjectMapper objectMapper;

    private ComparisonRequestDTO createTestComparisonRequest() {
        return ComparisonRequestDTO.builder()
                .symbol1("AAPL")
                .symbol2("GOOGL")
                .startDate(LocalDate.of(2023, 1, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .build();
    }

    private PerformanceComparisonDTO createTestPerformanceComparisonDTO() {
        return PerformanceComparisonDTO.builder()
                .symbol1("AAPL")
                .performance1(BigDecimal.valueOf(10.5))
                .symbol2("GOOGL")
                .performance2(BigDecimal.valueOf(15.0))
                .build();
    }

    @Test
    void testCompareSymbols() throws Exception {
        ComparisonRequestDTO request = createTestComparisonRequest();
        PerformanceComparisonDTO response = createTestPerformanceComparisonDTO();

        given(comparisonService.comparePerformance(
                anyString(), anyString(), any(LocalDate.class), any(LocalDate.class))
        ).willReturn(response);

        mockMvc.perform(post("/api/finance/comparison")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.symbol1", is("AAPL")))
                .andExpect(jsonPath("$.performance1", is(10.5)))
                .andExpect(jsonPath("$.symbol2", is("GOOGL")))
                .andExpect(jsonPath("$.performance2", is(15.0)));
    }

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
}
