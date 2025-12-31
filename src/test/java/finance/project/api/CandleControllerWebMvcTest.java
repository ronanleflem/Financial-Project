package finance.project.api;

import finance.project.api.controllers.CandleController;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(CandleController.class)
@ActiveProfiles("test")
class CandleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CandleService candleService;

    @MockBean
    private MarketstackService marketstackService;

    @MockBean
    private CurrencyLayerService currencyLayerService;

    @MockBean
    private CandleAggregationService candleAggregationService;

    @MockBean
    private VolumeBasedRolloverService volumeBasedRolloverService;

    @MockBean
    private TradeCompletedService tradeCompletedService;

    @MockBean
    private TradeCompletedMapper tradeCompletedMapper;

    @MockBean
    private DeltaLakeCandleReader deltaLakeCandleReader;

    @MockBean
    private BinanceService binanceService;

    @MockBean
    private SymbolService symbolService;

    @Test
    void shouldReturnNotFoundWhenTradeIsMissing() throws Exception {
        given(tradeCompletedService.getTradeById(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/finance/charts/from-trade")
                        .param("tradeId", "1")
                        .param("timeframe", "5min")
                        .param("symbol", "EURUSD")
                        .param("comparedSymbol", "USDJPY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Trade not found"))
                .andExpect(jsonPath("$.path").value("/api/finance/charts/from-trade"));
    }

    @Test
    void shouldReturnBadRequestWhenStartDateAfterEndDate() throws Exception {
        mockMvc.perform(get("/api/finance/charts/candles/date-time")
                        .param("symbol", "EURUSD")
                        .param("timeframe", "1h")
                        .param("startDate", "2024-01-02T00:00:00")
                        .param("endDate", "2024-01-01T00:00:00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("startDate must be before endDate"))
                .andExpect(jsonPath("$.path").value("/api/finance/charts/candles/date-time"));
    }
}
