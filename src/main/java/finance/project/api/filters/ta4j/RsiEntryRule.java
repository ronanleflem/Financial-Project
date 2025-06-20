package finance.project.api.filters.ta4j;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

public class RsiEntryRule extends AbstractRule {

    private final Indicator<Num> rsi;
    private final Num threshold;

    public RsiEntryRule(Indicator<Num> rsi, double threshold) {
        this.rsi = rsi;
        this.threshold = rsi.numOf(threshold);
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        return rsi.getValue(index).isLessThan(threshold);
    }
}