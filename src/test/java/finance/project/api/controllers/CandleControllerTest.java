package finance.project.api.controllers;

import finance.project.api.model.CandleDTO;
import finance.project.api.services.BinanceService;
import finance.project.api.services.CandleAggregationService;
import finance.project.api.services.CandleService;
import finance.project.api.services.CurrencyLayerService;
import finance.project.api.services.DeltaLakeCandleReader;
import finance.project.api.services.MarketstackService;
import finance.project.api.services.SymbolService;
import finance.project.api.services.TradeCompletedMapper;
import finance.project.api.services.TradeCompletedService;
import finance.project.api.services.VolumeBasedRolloverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import finance.project.api.model.SymbolDTO;
import org.springframework.test.web.servlet.MvcResult;


import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandleController.class)
@ActiveProfiles("test")
public class CandleControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    CandleService candleService;

    @MockBean
    MarketstackService marketstackService;

    @MockBean
    CurrencyLayerService currencyLayerService;

    @MockBean
    CandleAggregationService candleAggregationService;

    @MockBean
    VolumeBasedRolloverService volumeBasedRolloverService;

    @MockBean
    TradeCompletedService tradeCompletedService;

    @MockBean
    TradeCompletedMapper tradeCompletedMapper;

    @MockBean
    DeltaLakeCandleReader deltaLakeCandleReader;

    @MockBean
    BinanceService binanceService;

    @MockBean
    SymbolService symbolService;

    @Test
    void testGetChartData() throws Exception {

        SymbolDTO symbolDTO = SymbolDTO.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build();

        CandleDTO candleDTO = CandleDTO.builder()
                .open(BigDecimal.valueOf(100.00))
                .high(BigDecimal.valueOf(115.00))
                .close(BigDecimal.valueOf(110.00))
                .build();

        List<CandleDTO> candles = List.of(candleDTO);

        given(symbolService.getSymbolByCode("AAPL")).willReturn(symbolDTO);

        given(candleService.getCandles(symbolDTO, "daily")).willReturn(candles);

        MvcResult result = mockMvc.perform(get("/api/finance/charts")
                        .param("symbol", "AAPL")
                        .param("interval", "daily")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Print the result to debug
        //String content = result.getResponse().getContentAsString();
        //System.out.println("Response Content: " + content);

        // Perform the assertions
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
