package finance.project.api.filters;

import finance.project.api.entities.MarketData;
import finance.project.api.entities.Symbol;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.CandleDTO;

import java.util.List;

//Interface commune a tous les filtres
public interface Filter {
    /**
     * Évalue un filtre sur une liste de prix et retourne un score.
     * Score bas = conforme, Score élevé = anomalie ou filtre non respecté.
     *
     * @param priceChanges Liste des variations de prix.
     * @return Score du filtre (0 = conforme, > 0 = anomalie détectée).
     */
    int evaluate(TradeRequestDTO priceChanges);

    int evaluate(TradeRequestDTO tradeRequest, Symbol symbol, String timeframe, int period);

    int evaluate(TradeRequestDTO tradeRequest, String symbol, String timeframe, int period);

    /**
     * Evaluate the filter using preloaded candles.
     * Default implementation delegates to {@link #evaluate(TradeRequestDTO)} to
     * keep backward compatibility if a filter does not require candles.
     *
     * @param tradeRequest current trade request
     * @param candles      candles from the strategy context
     * @return score of the filter
     */
    default int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) {
        return evaluate(tradeRequest);
    }
}