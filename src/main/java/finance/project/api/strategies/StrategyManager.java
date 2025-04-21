package finance.project.api.strategies;

import finance.project.api.config.StrategyConfig;
import finance.project.api.entities.MarketData;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.model.TradeSignalTa4jDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.strategies.ta4j.TrendFollowingStrategy;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.TradingRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//Orchestration des stratégies actives
@Service
public class StrategyManager {

    private final List<BaseStrategy> allStrategies;
    private final StrategyConfig strategyConfig;

    private final TrendFollowingStrategy trendFollowingStrategy;


    public StrategyManager(List<BaseStrategy> allStrategies, StrategyConfig strategyConfig, TrendFollowingStrategy trendFollowingStrategy) {
        this.allStrategies = allStrategies;
        this.strategyConfig = strategyConfig;
        this.trendFollowingStrategy = trendFollowingStrategy;
    }

    public List<TradeSignalDTO> runStrategies(String symbol, String timeframe, int period) {
        List<TradeSignalDTO> trades = new ArrayList<>();
        List<BaseStrategy> enabledStrategies = allStrategies.stream()
                .filter(strategy -> strategyConfig.getEnabledStrategies().contains(strategy.getClass().getSimpleName()))
                .toList();

        for (BaseStrategy strategy : enabledStrategies) {
            List<TradeSignalDTO> tmp =  strategy.execute(symbol, timeframe, period);
            if(tmp != null) {
                trades.addAll(tmp);
            }
        }
        return trades;
    }

    public List<TradeSignalDTO> runTrendFollowing(String symbol, String timeframe, int period) {
        return trendFollowingStrategy.execute(symbol, timeframe, period);
    }


    /*
    public void runStrategies() {
        List<BaseStrategy> enabledStrategies = allStrategies.stream()
                .filter(strategy -> strategyConfig.getEnabledStrategies().contains(strategy.getClass().getSimpleName()))
                .toList();

        for (BaseStrategy strategy : enabledStrategies) {
            strategy.execute(marketData);
        }
    }

     */
}
