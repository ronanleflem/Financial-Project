package finance.project.api.scoring;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;

public interface ScoreRule {
    int calculateScore(TradeSignalDTO signal, MarketData marketData);

}
