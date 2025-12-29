package finance.project.api.strategies.ta4j;

import finance.project.api.config.StrategyConfig;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.services.TradeFilterService;
import finance.project.api.utils.StrategyResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TrendFollowingStrategyTest {

    @Test
    void trendFollowingStrategyBuildsDeterministicBuySignal() {
        List<CandleDTO> candles = buildCandles();
        StrategyConfig config = new StrategyConfig(new MockEnvironment());
        config.setEnabledFilters(Collections.emptyList());

        TradeFilterService tradeFilterService = new TradeFilterService(Collections.emptyList(), config, new CandleCacheManager());
        TrendFollowingStrategy strategy = new TrendFollowingStrategy(new TA4JService(), new CandleCacheManager(), tradeFilterService);

        StrategyResult result = strategy.execute("EURUSD", "1m", 1.0, 2.0, candles, 10);

        assertFalse(result.getSignals().isEmpty(), "Expected at least one BUY signal.");
        var signal = result.getSignals().getFirst();

        assertEquals(signal.getTradeType(), finance.project.api.model.TradeSignalDTO.TradeType.LONG);
        assertTrue(signal.getStopLoss() < signal.getEntryPrice(), "Stop loss should be below entry price.");
        assertTrue(signal.getTakeProfit() > signal.getEntryPrice(), "Take profit should be above entry price.");
        double rr = (signal.getTakeProfit() - signal.getEntryPrice()) / (signal.getEntryPrice() - signal.getStopLoss());
        assertEquals(2.0, rr, 1e-6);
    }

    private List<CandleDTO> buildCandles() {
        SymbolDTO symbol = SymbolDTO.builder()
                .symbol("EURUSD")
                .name("EURUSD")
                .market("FX")
                .build();
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);

        return IntStream.range(0, 120)
                .mapToObj(i -> {
                    double close = i < 60 ? 120.0 - i : 60.0 + (i - 60);
                    return CandleDTO.builder()
                            .symbol(symbol)
                            .timeframe("1m")
                            .date(start.plusMinutes(i))
                            .open(BigDecimal.valueOf(close - 0.5))
                            .high(BigDecimal.valueOf(close + 0.5))
                            .low(BigDecimal.valueOf(close - 1.0))
                            .close(BigDecimal.valueOf(close))
                            .volume(BigDecimal.valueOf(100 + i))
                            .build();
                })
                .toList();
    }
}
