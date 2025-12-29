package finance.project.api;

import finance.project.api.controllers.BacktestController;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.CandleService;
import finance.project.api.services.PerformanceService;
import finance.project.api.services.SymbolService;
import finance.project.api.services.TA4JService;
import finance.project.api.services.TradeCompletedMapper;
import finance.project.api.services.TradeCompletedService;
import finance.project.api.services.TradeService;
import finance.project.api.services.VolumeBasedRolloverService;
import finance.project.api.strategies.StrategyManager;
import finance.project.api.strategies.volume.EmaVolumeStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BacktestController.class)
@ActiveProfiles("test")
class BacktestControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TA4JService ta4JService;

    @MockBean
    private VolumeBasedRolloverService volumeBasedRolloverService;

    @MockBean
    private EmaVolumeStrategy emaVolumeStrategy;

    @MockBean
    private SymbolService symbolService;

    @MockBean
    private CandleService candleService;

    @MockBean
    private StrategyManager strategyManager;

    @MockBean
    private PerformanceService performanceService;

    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeCompletedService tradeCompletedService;

    @MockBean
    private TradeCompletedMapper tradeCompletedMapper;

    @MockBean
    private CandleCacheManager candleCacheManager;

    @Test
    void shouldReturnInternalServerErrorWhenStrategyFails() throws Exception {
        given(strategyManager.runStrategyByName(
                "TrendFollowing",
                "EURUSD",
                "1h",
                1000,
                null,
                null,
                null,
                null,
                java.time.LocalDateTime.parse("2024-01-01T00:00:00"),
                java.time.LocalDateTime.parse("2024-01-02T00:00:00")
        )).willThrow(new RuntimeException("Backtest failure"));

        mockMvc.perform(get("/run-strategy-by-name")
                        .param("strategyName", "TrendFollowing")
                        .param("symbol", "EURUSD")
                        .param("timeframe", "1h")
                        .param("startDate", "2024-01-01T00:00:00")
                        .param("endDate", "2024-01-02T00:00:00")
                        .param("period", "1000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Backtest failure"))
                .andExpect(jsonPath("$.path").value("/run-strategy-by-name"));
    }
}
