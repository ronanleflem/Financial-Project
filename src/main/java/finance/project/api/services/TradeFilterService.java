package finance.project.api.services;

import finance.project.api.config.StrategyConfig;
import finance.project.api.entities.MarketData;
import finance.project.api.filters.Filter;
import finance.project.api.filters.ta4j.FilterRuleAdapter;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.model.TradeRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

//Gère l'application des filtres
@Service
public class TradeFilterService {

    private final List<Filter> filters;
    private final StrategyConfig strategyConfig;
    private final CandleCacheManager candleCacheManager;
    private List<FilterRuleAdapter> ruleAdapters;

    public TradeFilterService(List<Filter> filters,
                              StrategyConfig strategyConfig,
                              CandleCacheManager candleCacheManager) {
        this.filters = filters;
        this.strategyConfig = strategyConfig;
        this.candleCacheManager = candleCacheManager;
    }

    public List<FilterRuleAdapter> buildFilterRules(TradeRequestDTO request, String symbol, String timeframe, int period) {
        this.ruleAdapters = filters.stream()
                .map(f -> new FilterRuleAdapter(f, request, symbol, timeframe, period, candleCacheManager))
                .toList();
        return ruleAdapters;
    }

    public List<FilterRuleAdapter> getRuleAdapters() {
        return ruleAdapters;
    }

    public int getTradeScore(TradeRequestDTO tradeRequest, MarketData marketData) {
        List<Filter> enabledFilters = filters.stream()
                .filter(filter -> strategyConfig.getEnabledFilters().contains(filter.getClass().getSimpleName()))
                .toList();

        int totalScore = 0;
        for (Filter filter : enabledFilters) {
            int filterScore = filter.evaluate(tradeRequest);
            int weight = strategyConfig.getFilterWeight(filter.getClass().getSimpleName());

            int weightedScore = filterScore * weight; // Appliquer la pondération
            totalScore += weightedScore;

            System.out.println("📌 [FILTER] " + filter.getClass().getSimpleName() +
                    " | Score brut : " + filterScore +
                    " | Poids : " + weight +
                    " | Score final : " + weightedScore);
        }

        return totalScore;
    }

    public boolean isTradeValid(TradeRequestDTO tradeRequest, MarketData marketData) {
        return getTradeScore(tradeRequest, marketData) >= 50;
    }

    public boolean isTradeValid(TradeRequestDTO tradeRequestDTO, String symbol, String timeframe, int period) {
        return getTradeScore(tradeRequestDTO, symbol,timeframe,period) >= 1;
    }

    private int getTradeScore(TradeRequestDTO tradeRequestDTO, String symbol, String timeframe, int period) {
        List<Filter> enabledFilters = filters.stream()
                .filter(filter -> strategyConfig.getEnabledFilters().contains(filter.getClass().getSimpleName()))
                .toList();

        List<CandleDTO> candles = candleCacheManager.getCandles(symbol, timeframe, Math.max(period, 500));

        int totalScore = 0;
        for (Filter filter : enabledFilters) {
            int filterScore = filter.evaluate(tradeRequestDTO, candles);
            int weight = strategyConfig.getFilterWeight(filter.getClass().getSimpleName());

            int weightedScore = filterScore * weight; // Appliquer la pondération
            totalScore += weightedScore;

            System.out.println("📌 [FILTER] " + filter.getClass().getSimpleName() +
                    " | Score brut : " + filterScore +
                    " | Poids : " + weight +
                    " | Score final : " + weightedScore);
        }

        return totalScore;
    }
}