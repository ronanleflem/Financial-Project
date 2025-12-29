package finance.project.api.strategies.volume;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.services.TA4JService;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmaVolumeStrategyTest {

    @Test
    void emaVolumeStrategyTriggersEntryAndExit() {
        List<CandleDTO> candles = buildCandles();
        TA4JService ta4jService = new TA4JService();
        BarSeries series = ta4jService.convertToTimeSeries(candles, "1m");

        EmaVolumeStrategy strategyBuilder = new EmaVolumeStrategy();
        Strategy strategy = strategyBuilder.buildStrategy(series);

        int entryIndex = IntStream.range(0, series.getBarCount())
                .filter(strategy::shouldEnter)
                .findFirst()
                .orElse(-1);
        int exitIndex = IntStream.range(0, series.getBarCount())
                .filter(i -> i > entryIndex && strategy.shouldExit(i))
                .findFirst()
                .orElse(-1);

        assertTrue(entryIndex >= 0, "Expected a BUY signal in the uptrend.");
        assertTrue(exitIndex > entryIndex, "Expected a SELL signal after the trend reversal.");
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
                    double close = i < 60 ? 1.0 + i : 61.0 - (i - 60);
                    double volume = 100 + i;
                    return CandleDTO.builder()
                            .symbol(symbol)
                            .timeframe("1m")
                            .date(start.plusMinutes(i))
                            .open(BigDecimal.valueOf(close - 0.2))
                            .high(BigDecimal.valueOf(close + 0.2))
                            .low(BigDecimal.valueOf(close - 0.4))
                            .close(BigDecimal.valueOf(close))
                            .volume(BigDecimal.valueOf(volume))
                            .build();
                })
                .toList();
    }
}
