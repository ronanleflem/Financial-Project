package finance.project.api.filters.ta4j;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Rule predicting MACD crossovers using linear regression.
 * <p>
 * For each evaluation, a simple linear regression is fitted on the last
 * {@code window} bars of the MACD minus its signal line. The regression
 * predicts the difference for the next bar. If the last observed difference
 * is negative while the prediction is positive, a bullish crossover is
 * anticipated. The opposite (positive last value, negative prediction)
 * signals a potential bearish crossover.
 */
public class LinearRegressionMacdCrossRule2 extends AbstractRule {

    private final Indicator<Num> macd;
    private final Indicator<Num> signal;
    private final int window;

    public LinearRegressionMacdCrossRule2(Indicator<Num> macd, Indicator<Num> signal, int window) {
        this.macd = macd;
        this.signal = signal;
        this.window = window;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord record) {
        if (index < window - 1) return false;

        SimpleRegression regression = new SimpleRegression();
        int start = index - window + 1;
        for (int i = 0; i < window; i++) {
            int idx = start + i;
            double diff = macd.getValue(idx).doubleValue() - signal.getValue(idx).doubleValue();
            regression.addData(i, diff);
        }

        double lastValue = macd.getValue(index).doubleValue() - signal.getValue(index).doubleValue();
        double predicted = regression.predict(window);

        return (lastValue < 0 && predicted > 0) || (lastValue > 0 && predicted < 0);
    }
}