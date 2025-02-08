package finance.project.api.strategies;

import finance.project.api.entities.Candle;
import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;

public interface Strategy {
    TradeSignalDTO generateTradeSignal(MarketData marketData);
}
