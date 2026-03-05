package finance.project.api.strategies.trend;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.services.TradeFilterService;
import finance.project.api.strategies.BaseStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

@Component
public class TrendContinuationStrategy extends BaseStrategy {

    @Autowired
    private CandleCacheManager candleCacheManager;

    @Autowired
    private TA4JService ta4jService;

    public TrendContinuationStrategy(TradeFilterService tradeFilterService) {
        super(tradeFilterService);
    }

    public List<TradeSignalDTO> execute(String symbol, String timeframe, int period) {
        List<CandleDTO> candles = candleCacheManager.getCandles(symbol, timeframe, period);

        if (candles.size() < period) {
            System.out.println("Pas assez de bougies pour executer la strategie.");
            return null;
        }

        BarSeries series = ta4jService.convertToTimeSeries(candles, timeframe);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(close, 200);

        int lastIndex = series.getEndIndex();
        double lastClose = close.getValue(lastIndex).doubleValue();
        double lastEma = ema.getValue(lastIndex).doubleValue();

        String action = lastClose > lastEma ? "BUY" : "SELL";

        TradeSignalDTO signal = new TradeSignalDTO(
                action.equals("BUY") ? TradeSignalDTO.TradeType.LONG : TradeSignalDTO.TradeType.SHORT,
                0, 0, 0, 0, symbol);
        TradeRequestDTO request = new TradeRequestDTO(signal);

        if (isTradeValid(request, symbol, timeframe, period)) {
            executeTrade(signal);
        } else {
            System.out.println("Signal rejete par les filtres : " + signal);
        }
        return null;
    }

    @Override
    protected TradeSignalDTO generateRawSignal(String symbol, String timeframe, int period) {
        return null;
    }
}
