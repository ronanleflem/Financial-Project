package finance.project.api.filters.trend;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

@Service
public class EMAFilter implements Filter {

    @Autowired
    private TA4JService ta4jService;

    @Autowired
    private CandleCacheManager candleCacheManager;

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, Symbol symbol, String timeframe, int period) {
        return 1;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, String symbol, String timeframe, int period) {
        List<CandleDTO> candles = candleCacheManager.getCandles(
                symbol,
                timeframe,
                period
        );
        BarSeries series = ta4jService.convertToTimeSeries(candles, timeframe);

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(close, 200);

        int lastIndex = series.getEndIndex();
        double lastClose = close.getValue(lastIndex).doubleValue();
        double lastEma = ema.getValue(lastIndex).doubleValue();

        // Exemple de règle : accepter seulement si prix > EMA200
        return lastClose > lastEma ? 1 : 0;
    }
    /*
    @Override
    public int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) {
        if (candles == null || candles.isEmpty()) {
            return 0;
        }
        BarSeries series = ta4jService.convertToTimeSeries(candles, candles.get(0).getTimeframe());

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(close, 50);

        int lastIndex = series.getEndIndex();
        double lastClose = close.getValue(lastIndex).doubleValue();
        double lastEma = ema.getValue(lastIndex).doubleValue();

        return lastClose > lastEma ? 1 : 0;
    }*/

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) {
        if (candles == null || candles.isEmpty()) {
            return 0;
        }
        BarSeries series = ta4jService.convertToTimeSeries(candles, candles.get(0).getTimeframe());

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema20 = new EMAIndicator(close, 20);
        EMAIndicator ema50 = new EMAIndicator(close, 50);

        int index = series.getEndIndex();

        Num ema20Now = ema20.getValue(index);
        Num ema50Now = ema50.getValue(index);
        Num ema20Prev = ema20.getValue(index - 1);

        boolean ema20AboveEma50 = ema20Now.isGreaterThan(ema50Now);
        boolean ema20Rising = ema20Now.isGreaterThan(ema20Prev);
        boolean closeAboveEma50 = close.getValue(index).isGreaterThan(ema50Now);

        // Condition réaliste cohérente avec un croisement haussier en cours ou récent
        if (ema20AboveEma50 && ema20Rising && closeAboveEma50) {
            return 1;
        }
        return 0;
    }

    @Override
    public int evaluate(TradeRequestDTO priceChanges) {
        return 1;
    }
}