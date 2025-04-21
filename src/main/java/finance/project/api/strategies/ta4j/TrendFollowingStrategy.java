package finance.project.api.strategies.ta4j;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;
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
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.StopLossRule;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrendFollowingStrategy {

    private final TA4JService ta4jService;
    private final CandleCacheManager candleCacheManager;

    private final TradeFilterService tradeFilterService;

    @Autowired
    public TrendFollowingStrategy(TA4JService ta4jService,
                                  CandleCacheManager candleCacheManager,
                                  TradeFilterService tradeFilterService) {
        this.ta4jService = ta4jService;
        this.candleCacheManager = candleCacheManager;
        this.tradeFilterService = tradeFilterService;
    }


    public List<TradeSignalTa4jDTO> execute(String symbol, String timeframe, int period) {
        // 1. Récupération des données
        List<CandleDTO> candles = candleCacheManager.getCandles(symbol, timeframe, period);
        BarSeries series = ta4jService.convertToTimeSeries(candles, timeframe);

        // 2. Construction de la stratégie TA4J
        Strategy strategy = buildTa4jStrategy(series);

        // 3. Exécution du backtest
        TradingRecord record = new BaseTradingRecord();
        List<TradeSignalTa4jDTO> signals = new ArrayList<>();

        for (int i = 0; i < series.getBarCount(); i++) {
            CandleDTO candle = candles.get(i);
            //FIXME :MODIF POUR AJOUTER LES FILTRES A LA STRAT TA4J
            TradeRequestDTO request = TradeRequestDTO.builder()
                    .symbol(candle.getSymbol().getSymbol())
                    //.timeframe(candle.getTimeframe())
                    .entryPrice(series.getBar(i).getClosePrice().doubleValue())
                    .entryTime(candle.getDate())
                    .build();

            // 🔎 Vérifier les filtres
            if (strategy.shouldEnter(i) && tradeFilterService.isTradeValid(request, new MarketData(candles))) {
                record.enter(i, series.getBar(i).getClosePrice(), series.getBar(i).getVolume());
                signals.add(buildTradeSignal("BUY", series.getBar(i).getClosePrice(), candle));
            } else if (strategy.shouldExit(i) && tradeFilterService.isTradeValid(request, new MarketData(candles))) {
                record.exit(i, series.getBar(i).getClosePrice(), series.getBar(i).getVolume());
                signals.add(buildTradeSignal("SELL", series.getBar(i).getClosePrice(), candle));
            }
        }

        return signals;
    }

    private Strategy buildTa4jStrategy(BarSeries series) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema20 = new EMAIndicator(close, 20);
        EMAIndicator ema50 = new EMAIndicator(close, 50);

        Rule entryRule = new CrossedUpIndicatorRule(ema20, ema50);
        Rule exitRule = new CrossedDownIndicatorRule(ema20, ema50)
                .or(new StopLossRule(close, 2.0))   // Exemple : Stop Loss 2%
                .or(new StopGainRule(close, 3.0));  // Exemple : Take Profit 3%

        return new BaseStrategy("TrendFollowing", entryRule, exitRule);
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
