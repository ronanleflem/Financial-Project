package finance.project.api.services;

import finance.project.api.entities.MarketData;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StrategyManager {

    private final List<BaseStrategy> strategies;

    public StrategyManager(List<BaseStrategy> strategies) {
        this.strategies = strategies;
    }

    public void runStrategies(MarketData marketData) {
        for (BaseStrategy strategy : strategies) {
            strategy.execute(marketData);
        }
    }
}