package finance.project.api.filters.ta4j;

import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.CandleCacheManager;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BaseTradingRecord;

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
        };
        TradeRequestDTO req = new TradeRequestDTO(TradeSignalDTO.builder()
                .tradeType(TradeSignalDTO.TradeType.LONG)
                .entryPrice(0)
                .stopLoss(0)
                .takeProfit(0)
                .confidenceScore(0)
                .symbol("EURUSD")
                .build());
        CandleCacheManager cache = new CandleCacheManager() {
            @Override
            public java.util.List<finance.project.api.model.CandleDTO> getCandles(String s, String tf, int p) {
                return java.util.Collections.emptyList();
            }
        };
        FilterRuleAdapter rule = new FilterRuleAdapter(f, req, "EURUSD", "M1", 10, cache);
        assertTrue(rule.isSatisfied(0, new BaseTradingRecord()));
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
        };
        TradeRequestDTO req = new TradeRequestDTO(TradeSignalDTO.builder()
                .tradeType(TradeSignalDTO.TradeType.LONG)
                .entryPrice(0)
                .stopLoss(0)
                .takeProfit(0)
                .confidenceScore(0)
                .symbol("EURUSD")
                .build());
        CandleCacheManager cache = new CandleCacheManager() {
            @Override
            public java.util.List<finance.project.api.model.CandleDTO> getCandles(String s, String tf, int p) {
                return java.util.Collections.emptyList();
            }
        };
        FilterRuleAdapter rule = new FilterRuleAdapter(f, req, "EURUSD", "M1", 10, cache);
        assertFalse(rule.isSatisfied(0, new BaseTradingRecord()));
    }
}
