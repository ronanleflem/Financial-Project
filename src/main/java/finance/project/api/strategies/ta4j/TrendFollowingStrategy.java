package finance.project.api.strategies.ta4j;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.model.TradeSignalTa4jDTO;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.services.TradeFilterService;
import finance.project.api.utils.StrategyResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.criteria.MaximumDrawdownCriterion;
import org.ta4j.core.criteria.NumberOfLosingPositionsCriterion;
import org.ta4j.core.criteria.NumberOfWinningPositionsCriterion;
import org.ta4j.core.criteria.pnl.AverageProfitCriterion;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.StopLossRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public StrategyResult execute(String symbol, String timeframe, int period) {
        List<CandleDTO> candles = candleCacheManager.getCandles(symbol, timeframe, period);
        BarSeries series = ta4jService.convertToTimeSeries(candles, timeframe);
        Strategy strategy = buildTa4jStrategy(series);

        TradingRecord record = new BaseTradingRecord();
        List<TradeSignalDTO> signals = new ArrayList<>();

        for (int i = 0; i < series.getBarCount(); i++) {
            CandleDTO candle = candles.get(i);
            Num price = series.getBar(i).getClosePrice();

            TradeSignalDTO tradeSignal = buildTradeFilterSignal("BUY", price, candle);
            TradeRequestDTO request = TradeRequestDTO.builder()
                    .tradeSignal(tradeSignal)
                    .timestamp(candle.getDate())
                    .bidPrice(price.doubleValue()) // ou adapter si tu stockes bid/ask séparément
                    .askPrice(price.doubleValue())
                    .volatility(0) // à calculer si nécessaire
                    .volume(candle.getVolume().doubleValue())
                    .symbol(symbol)
                    .build();

            // 🎯 Vérifie la stratégie + les filtres
            if (strategy.shouldEnter(i) /*&& tradeFilterService.isTradeValid(request, symbol,timeframe,period)*/) {
                record.enter(i, price, series.getBar(i).getVolume());
                signals.add(tradeSignal);
            } else if (strategy.shouldExit(i)) {
                record.exit(i, price, series.getBar(i).getVolume());
                TradeSignalDTO exitSignal = buildTradeFilterSignal("SELL", price, candle);
                signals.add(exitSignal);
            }
        }

        // 🔍 Ajoute les métriques
        Map<String, Double> performance = new HashMap<>();
        performance.put("totalReturn", new ProfitCriterion().calculate(series, record).doubleValue());
        performance.put("winRate", new NumberOfWinningPositionsCriterion().calculate(series, record).doubleValue());
        performance.put("lossRate", new NumberOfLosingPositionsCriterion().calculate(series, record).doubleValue());
        performance.put("maxDrawdown", new MaximumDrawdownCriterion().calculate(series, record).doubleValue());
        performance.put("averageTrade", new AverageProfitCriterion().calculate(series, record).doubleValue());

        return new StrategyResult(signals, performance);
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

    private TradeSignalDTO buildTradeFilterSignal(String direction, Num price, CandleDTO candle) {
        return TradeSignalDTO.builder()
                .tradeType(direction.equals("BUY") ? TradeSignalDTO.TradeType.LONG : TradeSignalDTO.TradeType.SHORT)
                .entryPrice(price.doubleValue())
                .stopLoss(0)        // À adapter selon ta stratégie
                .takeProfit(0)      // À adapter aussi
                .confidenceScore(1.0) // Valeur par défaut ou calculée
                .timestamp(candle.getDate())
                .build();
    }
}
