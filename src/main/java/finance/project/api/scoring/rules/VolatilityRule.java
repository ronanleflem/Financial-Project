package finance.project.api.scoring.rules;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.scoring.ScoreRule;
import org.springframework.stereotype.Component;

@Component
public class VolatilityRule implements ScoreRule {

    @Override
    public int calculateScore(TradeSignalDTO signal, MarketData marketData) {
        /*
        double atr = marketData.getAtr();

        if (atr > marketData.getAtrAverage()) {
            return 10;  // Bonus si la volatilité est favorable
        } else {
            return -5;  // Pénalité si volatilité trop faible
        }*/
        return 0;
    }
}