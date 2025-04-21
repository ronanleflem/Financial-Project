package finance.project.api.strategies;

import finance.project.api.config.StrategyConfig;
import finance.project.api.entities.MarketData;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeSignalDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//Orchestration des stratégies actives
@Service
public class StrategyManager {

    private final List<BaseStrategy> allStrategies;
    private final StrategyConfig strategyConfig;

    public StrategyManager(List<BaseStrategy> allStrategies, StrategyConfig strategyConfig) {
        this.allStrategies = allStrategies;
        this.strategyConfig = strategyConfig;
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
