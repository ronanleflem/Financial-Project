package finance.project.api.filters;

import finance.project.api.config.StrategyConfig;

public interface ScoringFilter extends Filter {
    default int getWeight(StrategyConfig strategyConfig) {
        return strategyConfig.getFilterWeight(this.getClass().getSimpleName());
    }
}