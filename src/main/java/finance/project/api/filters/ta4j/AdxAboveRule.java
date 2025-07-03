package finance.project.api.filters.ta4j;

import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Rule that is satisfied when the ADX value is above a given threshold.
 */
public class AdxAboveRule extends AbstractRule {

    private final ADXIndicator adx;
    private final Num threshold;

    public AdxAboveRule(ADXIndicator adx, double threshold) {
        this.adx = adx;
        this.threshold = adx.numOf(threshold);
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        return adx.getValue(index).isGreaterThanOrEqual(threshold);
    }
}
