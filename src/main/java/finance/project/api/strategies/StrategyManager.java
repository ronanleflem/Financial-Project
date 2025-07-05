package finance.project.api.strategies;

import finance.project.api.config.StrategyConfig;
import finance.project.api.entities.MarketData;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.model.TradeSignalTa4jDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.strategies.ta4j.MacdPredictionStrategy;
import finance.project.api.strategies.ta4j.TrendFollowingStrategy;
import finance.project.api.strategies.ta4j.ExplosionGridStrategy;
import finance.project.api.utils.StrategyResult;
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
    private final ExplosionGridStrategy explosionGridStrategy;
    private final MacdPredictionStrategy macdPredictionStrategy;

    public StrategyManager(List<BaseStrategy> allStrategies, StrategyConfig strategyConfig,
                           TrendFollowingStrategy trendFollowingStrategy,
                           ExplosionGridStrategy explosionGridStrategy,
                           MacdPredictionStrategy macdPredictionStrategy) {
        this.allStrategies = allStrategies;
        this.strategyConfig = strategyConfig;
        this.trendFollowingStrategy = trendFollowingStrategy;
        this.explosionGridStrategy = explosionGridStrategy;
        this.macdPredictionStrategy = macdPredictionStrategy;
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

    public StrategyResult runTrendFollowing(String symbol, String timeframe, int period, double slPercent, double rrRatio) {
        return trendFollowingStrategy.execute(symbol, timeframe, period, slPercent, rrRatio);
    }

    public StrategyResult runExplosionGrid(String symbol, String timeframe, int period,
                                           double explosionPct, double stepPct) {
        return explosionGridStrategy.execute(symbol, timeframe, period, explosionPct, stepPct);
    }

    public StrategyResult runMacdPrediction(String symbol, String timeframe, int period, double rrRatio) {
        return macdPredictionStrategy.execute(symbol, timeframe, period, rrRatio);
    }

    public StrategyResult runStrategyByName(String strategyName, String symbol, String timeframe, int period,
                                            Double slPercent, Double rrRatio,
                                            Double explosionPct, Double stepPct) {
        switch (strategyName.toLowerCase()) {
            case "trendfollowingstrategy" -> {
                double sl = slPercent != null ? slPercent : 1.0;
                double rr = rrRatio != null ? rrRatio : 2.0;
                return runTrendFollowing(symbol, timeframe, period, sl, rr);
            }
            case "explosiongridstrategy" -> {
                double exp = explosionPct != null ? explosionPct : 2.0;
                double step = stepPct != null ? stepPct : 5.0;
                return runExplosionGrid(symbol, timeframe, period, exp, step);
            }
            case "macdpredictionstrategy" -> {
                double rr = rrRatio != null ? rrRatio : 2.0;
                return runMacdPrediction(symbol, timeframe, period, rr);
            }
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategyName);
        }
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
