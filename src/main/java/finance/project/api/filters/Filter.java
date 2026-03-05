package finance.project.api.filters;

import finance.project.api.entities.Symbol;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;

import java.util.List;

// Interface commune a tous les filtres
public interface Filter {
    int evaluate(TradeRequestDTO priceChanges);

    int evaluate(TradeRequestDTO tradeRequest, Symbol symbol, String timeframe, int period);

    int evaluate(TradeRequestDTO tradeRequest, String symbol, String timeframe, int period);

    default int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) {
        return evaluate(tradeRequest);
    }
}
