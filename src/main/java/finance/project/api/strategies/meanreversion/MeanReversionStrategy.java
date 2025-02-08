package finance.project.api.strategies.meanreversion;

import finance.project.api.model.TradeSignalDTO;
import finance.project.api.scoring.TradeScoringService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.stereotype.Service;

@Service
public class MeanReversionStrategy extends BaseStrategy {

    public MeanReversionStrategy(TradeScoringService tradeScoringService) {
        super(tradeScoringService);
    }

    @Override
    protected TradeSignalDTO generateTradeSignal(MarketData marketData) {
        // Retour vers la moyenne
        if (marketData.getCurrentPrice() < marketData.getMovingAverage(50)) {
            return new TradeSignalDTO("BUY", marketData.getAsset(), marketData.getCurrentPrice());
        } else if (marketData.getCurrentPrice() > marketData.getMovingAverage(50)) {
            return new TradeSignalDTO("SELL", marketData.getAsset(), marketData.getCurrentPrice());
        }
        return null;
    }
}
