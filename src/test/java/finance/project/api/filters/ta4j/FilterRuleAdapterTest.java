package finance.project.api.filters.ta4j;

import finance.project.api.filters.Filter;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.TradeSignalDTO;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BaseTradingRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilterRuleAdapterTest {

    @Test
    void adapterReturnsTrueWhenFilterAccepts() {
        Filter f = new Filter() {
            @Override
            public int evaluate(TradeRequestDTO tradeRequest) { return 1; }
            @Override
            public int evaluate(TradeRequestDTO t, finance.project.api.entities.Symbol s, String tf, int p) { return 1; }
            @Override
            public int evaluate(TradeRequestDTO t, String s, String tf, int p) { return 1; }
            @Override
            public int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) {
                return candles.size() == 10 ? 1 : 0;
            }
        };
        TradeRequestDTO req = new TradeRequestDTO(TradeSignalDTO.builder()
                .tradeType(TradeSignalDTO.TradeType.LONG)
                .entryPrice(0)
                .stopLoss(0)
                .takeProfit(0)
                .confidenceScore(0)
                .symbol("EURUSD")
                .build());
        List<CandleDTO> candles = buildCandles(15);
        FilterRuleAdapter rule = new FilterRuleAdapter(f, req, "EURUSD", "1m", 10, candles, 0);

        assertTrue(rule.isSatisfied(12, new BaseTradingRecord()));
        assertEquals(12, rule.getSatisfiedIndex());
    }

    @Test
    void adapterReturnsFalseWhenFilterRejects() {
        Filter f = new Filter() {
            @Override
            public int evaluate(TradeRequestDTO tradeRequest) { return 0; }
            @Override
            public int evaluate(TradeRequestDTO t, finance.project.api.entities.Symbol s, String tf, int p) { return 0; }
            @Override
            public int evaluate(TradeRequestDTO t, String s, String tf, int p) { return 0; }
            @Override
            public int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) { return 0; }
        };
        TradeRequestDTO req = new TradeRequestDTO(TradeSignalDTO.builder()
                .tradeType(TradeSignalDTO.TradeType.LONG)
                .entryPrice(0)
                .stopLoss(0)
                .takeProfit(0)
                .confidenceScore(0)
                .symbol("EURUSD")
                .build());
        List<CandleDTO> candles = buildCandles(15);
        FilterRuleAdapter rule = new FilterRuleAdapter(f, req, "EURUSD", "1m", 10, candles, 0);

        assertFalse(rule.isSatisfied(12, new BaseTradingRecord()));
        assertEquals(-1, rule.getSatisfiedIndex());
    }

    private List<CandleDTO> buildCandles(int count) {
        SymbolDTO symbol = SymbolDTO.builder()
                .symbol("EURUSD")
                .name("EURUSD")
                .market("FX")
                .build();
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> CandleDTO.builder()
                        .symbol(symbol)
                        .timeframe("1m")
                        .date(start.plusMinutes(i))
                        .open(BigDecimal.valueOf(1.0 + i))
                        .high(BigDecimal.valueOf(1.2 + i))
                        .low(BigDecimal.valueOf(0.8 + i))
                        .close(BigDecimal.valueOf(1.0 + i))
                        .volume(BigDecimal.valueOf(100 + i))
                        .build())
                .toList();
    }
}
