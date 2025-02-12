package finance.project.api.strategies.statisticals;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.TradeFilterService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.stereotype.Service;

@Service
public class MeanReversionStrategy extends BaseStrategy {

    public MeanReversionStrategy(TradeFilterService tradeFilterService) {
        super(tradeFilterService);
    }

    @Override
    protected TradeSignalDTO generateRawSignal(MarketData marketData) {
        return null;
    }

    @Override
    public TradeSignalDTO generateTradeSignal(MarketData marketData) {
        return null;
    }
    /*
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
    */
}
