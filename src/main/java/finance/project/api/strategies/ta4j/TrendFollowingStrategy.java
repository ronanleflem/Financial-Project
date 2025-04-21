package finance.project.api.strategies.ta4j;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.model.TradeSignalTa4jDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.services.TradeFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrendFollowingStrategy {

    private final TA4JService ta4jService;
    private final CandleCacheManager candleCacheManager;

    @Autowired
    public TrendFollowingStrategy(TA4JService ta4jService,
                                  CandleCacheManager candleCacheManager) {
        this.ta4jService = ta4jService;
        this.candleCacheManager = candleCacheManager;
    }

    private Strategy buildTa4jStrategy(BarSeries series) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(close, 20);
        EMAIndicator longEma = new EMAIndicator(close, 50);

        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma);
        Rule exitRule = new CrossedDownIndicatorRule(shortEma, longEma);

        return new BaseStrategy("TrendFollowing", entryRule, exitRule);
    }

    public List<TradeSignalTa4jDTO> execute(String symbol, String timeframe, int period) {
        List<CandleDTO> candles = candleCacheManager.getCandles(symbol, timeframe, period);
        BarSeries series = ta4jService.convertToTimeSeries(candles, timeframe);

        List<TradeSignalTa4jDTO> signals = new ArrayList<>();
        Strategy strategy = buildTa4jStrategy(series);

        // Exécuter la stratégie sur toute la série
        TradingRecord record = new BaseTradingRecord();
        for (int i = 0; i < series.getBarCount(); i++) {
            if (strategy.shouldEnter(i)) {
                record.enter(i);
                signals.add(buildTradeSignal("BUY", series.getBar(i).getClosePrice(), candles.get(i)));
            } else if (strategy.shouldExit(i)) {
                record.exit(i);
                signals.add(buildTradeSignal("SELL", series.getBar(i).getClosePrice(), candles.get(i)));
            }
        }

        return signals;
    }

    private TradeSignalTa4jDTO buildTradeSignal(String direction, Num price, CandleDTO candle) {
        return TradeSignalTa4jDTO.builder()
                .symbol(candle.getSymbol().getName())
                .timeframe(candle.getTimeframe())
                .direction(direction)
                .price(price.doubleValue())
                .timestamp(candle.getDate())
                .build();
    }
}
