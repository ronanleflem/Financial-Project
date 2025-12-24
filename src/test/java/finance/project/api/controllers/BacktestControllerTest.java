package finance.project.api.controllers;

import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.CandleService;
import finance.project.api.services.PerformanceService;
import finance.project.api.services.StrategyManager;
import finance.project.api.services.TA4JService;
import finance.project.api.services.TradeCompletedMapper;
import finance.project.api.services.TradeCompletedService;
import finance.project.api.services.TradeService;
import finance.project.api.services.VolumeBasedRolloverService;
import finance.project.api.services.SymbolService;
import finance.project.api.strategies.volume.EmaVolumeStrategy;
import finance.project.api.utils.StrategyResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BacktestController.class)
class BacktestControllerTest {

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
    void runStrategyReturnsConfirmationMessage() throws Exception {
        given(strategyManager.runStrategies("AAPL", "1d", 1000))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/run-strategy")
                        .param("symbol", "AAPL")
                        .param("timeframe", "1d")
                        .param("period", "1000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Stratégies exécutées sur AAPL 1d"));
    }

    @Test
    void runTrendFollowingReturnsPayload() throws Exception {
        StrategyResult result = StrategyResult.builder()
                .signals(List.of())
                .completedTrades(List.of())
                .performance(Map.of("winRate", 0.75))
                .nameStrategy("TrendFollowing")
                .build();

        given(strategyManager.runTrendFollowing(anyString(), anyString(), anyInt(), anyDouble(), anyDouble()))
                .willReturn(result);

        mockMvc.perform(get("/trend-following")
                        .param("symbol", "AAPL")
                        .param("timeframe", "1d")
                        .param("comparedSymbol", "MSFT")
                        .param("period", "1000")
                        .param("slPercent", "1.0")
                        .param("rrRatio", "2.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.performance.winRate", is(0.75)))
                .andExpect(jsonPath("$.nameStrategy", is("TrendFollowing")));

        verify(tradeService).saveTrades(anyString(), any());
        verify(tradeCompletedService).saveCompletedTrades(anyString(), any(), anyString());
        verify(performanceService).savePerformance(anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void getTradesByStrategyReturnsNoContentWhenEmpty() throws Exception {
        given(tradeCompletedService.getTradesByStrategyAndRunId("TrendFollowing", "run-1"))
                .willReturn(List.of());

        mockMvc.perform(get("/get-trades-strategy")
                        .param("strategyName", "TrendFollowing")
                        .param("runId", "run-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verifyNoInteractions(tradeCompletedMapper);
    }
}
