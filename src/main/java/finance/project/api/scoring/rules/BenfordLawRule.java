package finance.project.api.scoring.rules;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.scoring.ScoreRule;
import org.springframework.stereotype.Component;

@Component
public class BenfordLawRule implements ScoreRule {

    @Override
    public int calculateScore(TradeSignalDTO signal, MarketData marketData) {
        //boolean isBenfordValid = marketData.checkBenfordLaw(signal.getTimestamp());
        boolean isBenfordValid = false;
        return isBenfordValid ? 10 : -20;  // Grosse pénalité si la structure des bougies est suspecte
    }
}