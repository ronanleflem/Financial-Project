package finance.project.api.filters.ta4j;

import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.rules.AbstractRule;

/**
 * Checks if current volume is above its moving average.
 */
public class VolumeAboveAverageRule extends AbstractRule {

    private final VolumeIndicator volume;
    private final SMAIndicator average;

    public VolumeAboveAverageRule(VolumeIndicator volume, int period) {
        this.volume = volume;
        this.average = new SMAIndicator(volume, period);
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        return volume.getValue(index).isGreaterThan(average.getValue(index));
    }
}