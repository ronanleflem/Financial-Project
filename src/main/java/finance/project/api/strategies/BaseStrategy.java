package finance.project.api.strategies;

import finance.project.api.entities.MarketData;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TradeFilterService;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseStrategy implements Strategy {

    protected final TradeFilterService tradeFilterService;
    private final CandleCacheManager candleCacheManager;

    public BaseStrategy(TradeFilterService tradeFilterService, CandleCacheManager candleCacheManager) {
        this.tradeFilterService = tradeFilterService;
        this.candleCacheManager = candleCacheManager;
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

    public List<TradeSignalDTO> execute(String symbol, String timeframe, int lookbackPeriod) {
        List<TradeSignalDTO> executedTrades = new ArrayList<>();
        List<CandleDTO> candles = candleCacheManager.getCandles(symbol, timeframe, lookbackPeriod);

        if (candles.size() < lookbackPeriod) {
            System.out.println("❌ Pas assez de données pour backtest.");
            return executedTrades;
        }

        for (int i = lookbackPeriod; i < candles.size(); i++) {
            List<CandleDTO> window = candles.subList(i - lookbackPeriod, i);
            CandleDTO currentCandle = candles.get(i);

            TradeSignalDTO signal = generateRawSignal(symbol,timeframe,lookbackPeriod);

            if (signal != null && isTradeValid(new TradeRequestDTO(signal), symbol,timeframe,lookbackPeriod)) {
                executedTrades.add(signal);
            }
            else {
                System.out.println("🚫 Signal rejeté par les filtres.");
            }
        }

        return executedTrades;
    }

    public void executeOld(String symbol, String timeframe, int period) {
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
