package finance.project.api.strategies;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.scoring.TradeScoringService;
import finance.project.api.services.TradeFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    }

    private void executeTrade(TradeSignalDTO signal) {
        System.out.println("✅ Trade exécuté : " + signal);
    }
}
