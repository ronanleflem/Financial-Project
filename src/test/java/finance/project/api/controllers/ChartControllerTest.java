package finance.project.api.controllers;

import finance.project.api.services.ChartService;
import finance.project.api.services.ChartServiceImpl;
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

@WebMvcTest(ChartController.class)
public class ChartControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    ChartService chartService;

    ChartServiceImpl chartServiceImpl;

    @BeforeEach
    void setUp() {
        chartServiceImpl = new ChartServiceImpl();
    }

    @Test
    void testGetChartData() throws Exception {
        // Arrange
        given(chartService.getChartData("AAPL", "daily")).willReturn(chartServiceImpl.getChartData("AAPL", "daily"));

        // Act & Assert
        mockMvc.perform(get("/api/finance/charts")
                        .param("symbol", "AAPL")
                        .param("interval", "daily")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(2)));
                //.andExpect(jsonPath("$[0].open", is(100.00)))
                //.andExpect(jsonPath("$[0].high", is(115.00)))
                //.andExpect(jsonPath("$[0].close", is(110.00)));

    }

}
