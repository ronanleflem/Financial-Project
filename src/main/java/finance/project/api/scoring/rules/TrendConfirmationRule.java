package finance.project.api.scoring.rules;

import finance.project.api.model.TradeSignalDTO;
import finance.project.api.scoring.ScoreRule;
import org.springframework.stereotype.Component;

@Component
public class TrendConfirmationRule implements ScoreRule {

    @Override
    public int calculateScore(TradeSignalDTO signal, MarketData marketData) {
        double ema50 = marketData.getEma(50);
        double ema200 = marketData.getEma(200);

        if (ema50 > ema200 && signal.getDirection().equals("BUY")) {
            return 15;  // Bonus si achat dans une tendance haussière
        } else if (ema50 < ema200 && signal.getDirection().equals("SELL")) {
            return 15;  // Bonus si vente dans une tendance baissière
        }
        return -10;  // Pénalité si on va contre la tendance
    }
}
