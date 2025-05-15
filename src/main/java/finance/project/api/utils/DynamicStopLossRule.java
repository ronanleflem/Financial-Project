package finance.project.api.utils;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

import java.util.List;

public class DynamicStopLossRule extends AbstractRule {

    private final BarSeries series;
    private final int lookback;
    private final double rrRatio;

    public DynamicStopLossRule(BarSeries series, int lookback, double rrRatio) {
        this.series = series;
        this.lookback = lookback;
        this.rrRatio = rrRatio;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || tradingRecord.isClosed()) return false;

        double entryPrice = tradingRecord.getCurrentPosition().getEntry().getNetPrice().doubleValue();
        boolean isLong = tradingRecord.getCurrentPosition().getStartingType().name().equals("BUY"); // FIXME : Pas sur

        int from = Math.max(0, index - lookback);
        List<Bar> lookbackBars = series.getBarData().subList(from, index);

        double stopLoss;
        double takeProfit;

        if (isLong) {
            double lowestLow = lookbackBars.stream()
                    .mapToDouble(b -> b.getLowPrice().doubleValue())
                    .min().orElse(entryPrice * 0.99);
            stopLoss = lowestLow;
            takeProfit = entryPrice + (entryPrice - stopLoss) * rrRatio;
            return series.getBar(index).getClosePrice().doubleValue() <= stopLoss
                    || series.getBar(index).getClosePrice().doubleValue() >= takeProfit;
        } else {
            double highestHigh = lookbackBars.stream()
                    .mapToDouble(b -> b.getHighPrice().doubleValue())
                    .max().orElse(entryPrice * 1.01);
            stopLoss = highestHigh;
            takeProfit = entryPrice - (highestHigh - entryPrice) * rrRatio;
            return series.getBar(index).getClosePrice().doubleValue() >= stopLoss
                    || series.getBar(index).getClosePrice().doubleValue() <= takeProfit;
        }
    }
}