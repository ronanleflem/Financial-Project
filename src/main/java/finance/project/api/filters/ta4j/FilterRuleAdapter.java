package finance.project.api.filters.ta4j;

import finance.project.api.filters.Filter;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.services.CandleCacheManager;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

import java.util.List;

/**
 * Adapter to use existing {@link Filter} implementations as TA4J rules.
 */
public class FilterRuleAdapter extends AbstractRule {

    private final Filter filter;
    private final String symbol;
    private final String timeframe;
    private final int period;
    private final TradeRequestDTO tradeRequest;
    private final List<CandleDTO> candles;
    private final int tolerance;
    private int satisfiedIndex = -1;

    public FilterRuleAdapter(Filter filter, TradeRequestDTO tradeRequest,
                             String symbol, String timeframe, int period,
                             List<CandleDTO> candles,
                             int tolerance) {
        this.filter = filter;
        this.tradeRequest = tradeRequest;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.period = period;
        this.candles = candles;
        this.tolerance = tolerance;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord record) {
        if (index < period) {
            satisfiedIndex = -1;
            return false;
        }

        for (int offset = -tolerance; offset <= tolerance; offset++) {
            int checkIndex = index + offset;
            if (checkIndex < period || checkIndex > candles.size()) continue;
            int start = Math.max(0, checkIndex - period);
            java.util.List<CandleDTO> window = candles.subList(start, checkIndex);
            int score = filter.evaluate(tradeRequest, window);
            if (score >= 1) {
                satisfiedIndex = checkIndex;
                return true;
            }
        }
        satisfiedIndex = -1;
        return false;
    }

    public int getSatisfiedIndex() {
        return satisfiedIndex;
    }
}
