package finance.project.api.filters.ta4j;


import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Rule validating that ATR is rising compared to the previous bar or lookback period
 */
public class AtrRisingRule extends AbstractRule {

    private final ATRIndicator atr;
    private final int lookback;
    /*
    public AtrRisingRule(ATRIndicator atr) {
        this.atr = atr;
    }*/

    public AtrRisingRule(ATRIndicator atr, int lookback) {
        this.atr = atr;
        this.lookback = lookback;
    }
    /*
    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (index < 1) return false;
        return atr.getValue(index).isGreaterThan(atr.getValue(index - 1));
    }*/

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (index < lookback) {
            return false;
        }
        Num start = atr.getValue(index - lookback);
        Num end = atr.getValue(index);
        return end.isGreaterThan(start);
    }
}