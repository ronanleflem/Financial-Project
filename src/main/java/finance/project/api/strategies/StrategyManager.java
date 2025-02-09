package finance.project.api.strategies;

import finance.project.api.config.StrategyConfig;
import finance.project.api.entities.MarketData;
import org.springframework.stereotype.Service;
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

    public void runStrategies(MarketData marketData) {
        List<BaseStrategy> enabledStrategies = allStrategies.stream()
                .filter(strategy -> strategyConfig.getEnabledStrategies().contains(strategy.getClass().getSimpleName()))
                .toList();

        for (BaseStrategy strategy : enabledStrategies) {
            strategy.execute(marketData);
        }
    }
}
