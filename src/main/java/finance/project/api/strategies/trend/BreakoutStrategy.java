package finance.project.api.strategies.trend;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.scoring.TradeScoringService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.stereotype.Service;

@Service
public class BreakoutStrategy extends BaseStrategy {

    public BreakoutStrategy(TradeScoringService tradeScoringService) {
        super(tradeScoringService);
    }

    @Override
    protected TradeSignalDTO generateTradeSignal(MarketData marketData) {
        // Détection d'un breakout
        /*
        if (marketData.getCurrentPrice() > marketData.getHighOfLastXPeriods(20)) {
            return new TradeSignalDTO("BUY", marketData.getAsset(), marketData.getCurrentPrice());
        }*/
        return null;
    }
}