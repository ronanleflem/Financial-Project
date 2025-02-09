package finance.project.api.filters;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeRequestDTO;

//Interface commune a tous les filtres
public interface Filter {
    int evaluate(TradeRequestDTO tradeRequest, MarketData marketData);
}