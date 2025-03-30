package finance.project.api.strategies.volume;

import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

@Service
public class EmaVolumeStrategy {

    public Strategy buildStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 10);
        EMAIndicator longEma = new EMAIndicator(closePrice, 50);
        VolumeIndicator volume = new VolumeIndicator(series);

        // Condition d'achat : EMA10 coupe EMA50 à la hausse + volume au-dessus de la moyenne
        Rule buyingRule = new OverIndicatorRule(shortEma, longEma)
                .and(new OverIndicatorRule(volume, new SMAIndicator(volume, 20)));

        // Condition de vente : EMA10 coupe EMA50 à la baisse
        Rule sellingRule = new UnderIndicatorRule(shortEma, longEma);

        return new BaseStrategy(buyingRule, sellingRule);
    }
}
