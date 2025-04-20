package finance.project.api.strategies;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.TradeFilterService;

public abstract class BaseStrategy implements Strategy {

    protected final TradeFilterService tradeFilterService;

    public BaseStrategy(TradeFilterService tradeFilterService) {
        this.tradeFilterService = tradeFilterService;
    }

    public boolean isTradeValid(TradeRequestDTO tradeRequest, MarketData marketData) {
        return tradeFilterService.isTradeValid(tradeRequest, marketData);
    }

    protected abstract TradeSignalDTO generateRawSignal(MarketData marketData);

    public void execute(MarketData marketData) {

        TradeSignalDTO rawSignal = generateRawSignal(marketData);
        if (rawSignal != null && isTradeValid(new TradeRequestDTO(rawSignal), marketData)) {
            executeTrade(rawSignal);
        } else {
            System.out.println("🚫 Signal rejeté par les filtres.");
        }
        System.out.println("🚫 Signal rejeté par les filtres.");
    }

    public void execute(String symbol, String timeframe, int period) {
        TradeSignalDTO rawSignal = generateRawSignal(symbol, timeframe, period);
        if (rawSignal != null && isTradeValid(new TradeRequestDTO(rawSignal), symbol, timeframe, period)) {
            executeTrade(rawSignal);
        } else {
            System.out.println("🚫 Signal rejeté par les filtres.");
        }
    }

    protected boolean isTradeValid(TradeRequestDTO tradeRequestDTO, String symbol, String timeframe, int period) {
        return tradeFilterService.isTradeValid(tradeRequestDTO, symbol,timeframe, period);
    }

    protected abstract TradeSignalDTO generateRawSignal(String symbol, String timeframe, int period);


    protected void executeTrade(TradeSignalDTO signal) {
        System.out.println("✅ Trade exécuté : " + signal);
    }
}
