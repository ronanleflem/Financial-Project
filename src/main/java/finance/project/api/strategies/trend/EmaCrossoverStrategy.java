package finance.project.api.strategies.trend;

import finance.project.api.entities.Candle;
import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.TradeFilterService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

@Component
public class EmaCrossoverStrategy  {

    public EmaCrossoverStrategy(TradeFilterService tradeFilterService) {

    }
    /*
    @Override
    protected TradeSignalDTO generateRawSignal(MarketData marketData) {

        ClosePriceIndicator closePrice = new ClosePriceIndicator(marketData.getTimeSeries());
        EMAIndicator ema50 = new EMAIndicator(closePrice, 50);

        int lastIndex = marketData.getTimeSeries().getEndIndex();
        if (closePrice.getValue(lastIndex).isGreaterThan(ema50.getValue(lastIndex))) {
            return new TradeSignalDTO("BUY", marketData.getCurrentCandle());
        } else {
            return new TradeSignalDTO("SELL", marketData.getCurrentCandle());
        }
    }

    @Override
    public TradeSignalDTO generateTradeSignal(MarketData marketData) {
        return null;
    }*/
}