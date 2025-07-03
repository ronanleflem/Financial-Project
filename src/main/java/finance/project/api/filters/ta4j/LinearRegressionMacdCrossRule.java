package finance.project.api.filters.ta4j;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

/**
 * Predicts a MACD cross using linear regression on the MACD histogram.
 * If {@code bullish} is true, it checks if the predicted histogram for the next bar crosses
 * from negative to positive. Otherwise it checks for a cross from positive to negative.
 */
public class LinearRegressionMacdCrossRule extends AbstractRule {

    private final Indicator<Num> macd;
    private final Indicator<Num> signal;
    private final int lookback;
    private final boolean bullish;

    public LinearRegressionMacdCrossRule(Indicator<Num> macd,
                                         Indicator<Num> signal,
                                         int lookback,
                                         boolean bullish) {
        this.macd = macd;
        this.signal = signal;
        this.lookback = lookback;
        this.bullish = bullish;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (index < lookback) return false;

        // Build histogram values for regression
        double[] y = new double[lookback];
        for (int i = 0; i < lookback; i++) {
            int idx = index - lookback + 1 + i;
            y[i] = macd.getValue(idx).doubleValue() - signal.getValue(idx).doubleValue();
        }

        // Simple linear regression to predict the next value
        double meanX = 0.0;
        double meanY = 0.0;
        for (int i = 0; i < lookback; i++) {
            meanX += i;
            meanY += y[i];
        }
        meanX /= lookback;
        meanY /= lookback;

        double num = 0.0;
        double den = 0.0;
        for (int i = 0; i < lookback; i++) {
            double dx = i - meanX;
            num += dx * (y[i] - meanY);
            den += dx * dx;
        }
        double slope = den == 0 ? 0 : num / den;
        double intercept = meanY - slope * meanX;

        double predicted = intercept + slope * lookback; // next index
        double current = macd.getValue(index).doubleValue() - signal.getValue(index).doubleValue();

        if (bullish) {
            return current < 0 && predicted > 0;
        } else {
            return current > 0 && predicted < 0;
        }
    }
}