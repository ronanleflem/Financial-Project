package finance.project.api.strategies.trend;

import finance.project.api.entities.Candle;
import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.TradeFilterService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.stereotype.Component;

@Component
public class EmaCrossoverStrategy extends BaseStrategy {

    public EmaCrossoverStrategy(TradeFilterService tradeFilterService) {
        super(tradeFilterService);
    }

    @Override
    protected TradeSignalDTO generateRawSignal(MarketData marketData) {
        /*
        Candle latestCandle = marketData.getLatestCandle();
        if (latestCandle == null) return null;

        double ema50 = marketData.getEma(50);
        double ema200 = marketData.getEma(200);

        if (ema50 > ema200) {
            return new TradeSignalDTO("BUY", latestCandle.getTimestamp());
        } else if (ema50 < ema200) {
            return new TradeSignalDTO("SELL", latestCandle.getTimestamp());
        }*/
        return null;
    }

    private double calculateEMA(int period, Candle candle, MarketData marketData) {
        // Implémente le calcul de l'EMA selon ta logique
        return 0.0;
    }

    @Override
    public TradeSignalDTO generateTradeSignal(MarketData marketData) {
        return null;
    }
}