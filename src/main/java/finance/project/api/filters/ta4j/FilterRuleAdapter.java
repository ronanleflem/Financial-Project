package finance.project.api.filters.ta4j;

import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
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

    public FilterRuleAdapter(Filter filter, TradeRequestDTO tradeRequest,
                             String symbol, String timeframe, int period) {
        this.filter = filter;
        this.tradeRequest = tradeRequest;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.period = period;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord record) {
        int score = filter.evaluate(tradeRequest, symbol, timeframe, period);
        return score >= 1;
    }
}
