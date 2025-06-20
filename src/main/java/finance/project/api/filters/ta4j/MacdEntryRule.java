package finance.project.api.filters.ta4j;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

public class MacdEntryRule extends AbstractRule {

    private final Indicator<Num> macd;
    private final Indicator<Num> signal;

    public MacdEntryRule(Indicator<Num> macd, Indicator<Num> signal) {
        this.macd = macd;
        this.signal = signal;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (index < 1) return false;
        Num prevMacd = macd.getValue(index - 1);
        Num prevSignal = signal.getValue(index - 1);
        Num currMacd = macd.getValue(index);
        Num currSignal = signal.getValue(index);
        return prevMacd.isLessThan(prevSignal) && currMacd.isGreaterThan(currSignal);
    }
}