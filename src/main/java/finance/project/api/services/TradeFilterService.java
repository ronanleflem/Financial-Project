package finance.project.api.services;

import finance.project.api.config.StrategyConfig;
import finance.project.api.entities.MarketData;
import finance.project.api.filters.Filter;
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

    public TradeFilterService(List<Filter> filters, StrategyConfig strategyConfig) {
        this.filters = filters;
        this.strategyConfig = strategyConfig;
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
        return getTradeScore(tradeRequestDTO, symbol,timeframe,period) >= 50;
    }

    private int getTradeScore(TradeRequestDTO tradeRequestDTO, String symbol, String timeframe, int period) {
        List<Filter> enabledFilters = filters.stream()
                .filter(filter -> strategyConfig.getEnabledFilters().contains(filter.getClass().getSimpleName()))
                .toList();

        int totalScore = 0;
        for (Filter filter : enabledFilters) {
            int filterScore = filter.evaluate(tradeRequestDTO);
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