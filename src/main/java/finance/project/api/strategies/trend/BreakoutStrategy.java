package finance.project.api.strategies.trend;

import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.TradeFilterService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.stereotype.Service;

@Service
public class BreakoutStrategy extends BaseStrategy {

    public BreakoutStrategy(TradeFilterService tradeFilterService) {
        super(tradeFilterService);
    }

    @Override
    protected TradeSignalDTO generateRawSignal(String symbol, String timeframe, int period) {
        return null;
    }
}
