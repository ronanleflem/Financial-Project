package finance.project.api.strategies.statisticals;

import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.TradeFilterService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.stereotype.Service;

@Service
public class MeanReversionStrategy extends BaseStrategy {
    // Canonical MeanReversionStrategy implementation (kept in statisticals package).

    public MeanReversionStrategy(TradeFilterService tradeFilterService) {
        super(tradeFilterService);
    }

    @Override
    protected TradeSignalDTO generateRawSignal(String symbol, String timeframe, int period) {
        return null;
    }
}
