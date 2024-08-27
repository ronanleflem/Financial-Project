package finance.project.api.controllers;

import finance.project.api.services.CandleService;
import finance.project.api.services.CandleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;


import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandleController.class)
public class CandleControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    CandleService chartService;

    CandleServiceImpl chartServiceImpl;

    @BeforeEach
    void setUp() {
        chartServiceImpl = new CandleServiceImpl();
    }

    @Test
    void testGetChartData() throws Exception {
        // Arrange
        given(chartService.getCandles("AAPL", "daily")).willReturn(chartServiceImpl.getCandles("AAPL", "daily"));

        // Act & Assert
        mockMvc.perform(get("/api/finance/charts")
                        .param("symbol", "AAPL")
                        .param("interval", "daily")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].open", is(100.00)))
                .andExpect(jsonPath("$[0].high", is(115.00)))
                .andExpect(jsonPath("$[0].close", is(110.00)));

    }

}
