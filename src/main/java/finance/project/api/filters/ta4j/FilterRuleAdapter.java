package finance.project.api.filters.ta4j;

import finance.project.api.filters.Filter;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.services.CandleCacheManager;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * Adapter to use existing {@link Filter} implementations as TA4J rules.
 */
public class FilterRuleAdapter extends AbstractRule {

    private final Filter filter;
    private final String symbol;
    private final String timeframe;
    private final int period;
    private final TradeRequestDTO tradeRequest;
    private final CandleCacheManager candleCacheManager;

    public FilterRuleAdapter(Filter filter, TradeRequestDTO tradeRequest,
                             String symbol, String timeframe, int period,
                             CandleCacheManager candleCacheManager) {
        this.filter = filter;
        this.tradeRequest = tradeRequest;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.period = period;
        this.candleCacheManager = candleCacheManager;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord record) {
        int lookback = Math.max(period, 500);
        java.util.List<CandleDTO> candles = candleCacheManager.getCandles(symbol, timeframe, lookback);
        int score = filter.evaluate(tradeRequest, candles);
        return score >= 1;
    }
}
