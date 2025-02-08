package finance.project.api.strategies.trend;

import finance.project.api.entities.Candle;
import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.scoring.TradeScoringService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmaCrossoverStrategy extends BaseStrategy {

    public EmaCrossoverStrategy(TradeScoringService tradeScoringService) {
        super(tradeScoringService);
    }

    @Override
    protected TradeSignalDTO generateTradeSignal(MarketData marketData) {
        /*
        Candle latestCandle = marketData.getLatestCandle();
        if (latestCandle == null) return null;

        double ema50 = calculateEMA(50, latestCandle, marketData);
        double ema200 = calculateEMA(200, latestCandle, marketData);

        TradeSignalDTO signal = null;
        if (ema50 > ema200) {
            signal = new TradeSignalDTO("BUY", latestCandle.getTimestamp());
        } else if (ema50 < ema200) {
            signal = new TradeSignalDTO("SELL", latestCandle.getTimestamp());
        }

        // Vérification via le TradeScoringService
        return (signal != null && tradeScoringService.isTradeValid(signal, marketData)) ? signal : null;*/
        return null;
    }

    private double calculateEMA(int period, Candle candle, MarketData marketData) {
        // Implémente le calcul de l'EMA selon ta logique
        return 0.0;
    }
}