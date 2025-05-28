package finance.project.api.strategies.ta4j;

import finance.project.api.model.*;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.services.TradeFilterService;
import finance.project.api.utils.DynamicStopLossRule;
import finance.project.api.utils.StrategyResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.MaximumDrawdownCriterion;
import org.ta4j.core.criteria.NumberOfLosingPositionsCriterion;
import org.ta4j.core.criteria.NumberOfWinningPositionsCriterion;
import org.ta4j.core.criteria.ReturnOverMaxDrawdownCriterion;
import org.ta4j.core.criteria.pnl.AverageProfitCriterion;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.StopLossRule;

import java.util.*;
import java.util.stream.Collectors;

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

    public StrategyResult execute(String symbol, String timeframe, int period, double slPercent, double rrRatio) {
        List<CandleDTO> candles = candleCacheManager.getCandles(symbol, timeframe, period)
                .stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(ArrayList::new),
                        list -> {
                            if (list.size() >= 2 && list.get(0).getDate().isAfter(list.get(1).getDate())) {
                                Collections.reverse(list);
                            }
                            return list;
                        }
                ));
        BarSeries series = ta4jService.convertToTimeSeries(candles, timeframe);
        Strategy strategy = buildTa4jStrategy(series, 20, rrRatio);

        // 3. Backtest via TA4J
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord record = manager.run(strategy);

        // 4. Analyse des résultats
        ProfitCriterion profitCriterion = new ProfitCriterion();
        ReturnOverMaxDrawdownCriterion drawdownCriterion = new ReturnOverMaxDrawdownCriterion();

        Strategy strategylive = buildTa4jStrategy(series, 20, rrRatio);
        TradingRecord recordlive = new BaseTradingRecord();
        List<TradeSignalDTO> signals = new ArrayList<>();

        List<CompletedTradeDTO> completedTrades = new ArrayList<>();
        TradeSignalDTO entrySignal = null;

        for (int i = 0; i < series.getBarCount(); i++) {
            CandleDTO candle = candles.get(i);
            Num price = series.getBar(i).getClosePrice();

            // 🎯 Vérifie la stratégie + les filtres
            if (strategy.shouldEnter(i) && recordlive.isClosed()) { //&& tradeFilterService.isTradeValid(request, symbol,timeframe,period)
                recordlive.enter(i, price, series.getBar(i).getVolume());
                List<CandleDTO> recentCandles = candles.subList(Math.max(0, i - 20), i); // Les 20 dernières bougies
                entrySignal = buildTradeFilterSignal("BUY", price, candle, recentCandles, rrRatio,symbol);
                signals.add(entrySignal);

            } else if (strategy.shouldExit(i) && !recordlive.isClosed() && entrySignal != null) {
                recordlive.exit(i, price, series.getBar(i).getVolume());
                List<CandleDTO> recentExitCandles = candles.subList(Math.max(0, i - 20), i); // Les 20 dernières bougies
                TradeExitDTO exitSignal = new TradeExitDTO(price.doubleValue(),candle.getDate());
                completedTrades.add(new CompletedTradeDTO(entrySignal, exitSignal));
                entrySignal = null; // Reset pour la prochaine position
            }
        }

        // 🔍 Ajoute les métriques
        Map<String, Double> performance = new HashMap<>();
        performance.put("totalReturn", new ProfitCriterion().calculate(series, recordlive).doubleValue());
        performance.put("winRate", new NumberOfWinningPositionsCriterion().calculate(series, recordlive).doubleValue());
        performance.put("lossRate", new NumberOfLosingPositionsCriterion().calculate(series, recordlive).doubleValue());
        performance.put("maxDrawdown", new MaximumDrawdownCriterion().calculate(series, recordlive).doubleValue());
        performance.put("averageTrade", new AverageProfitCriterion().calculate(series, recordlive).doubleValue());

        return new StrategyResult(signals, completedTrades, performance,"TrendFollowing");
        //return new StrategyResult();
    }


    private Strategy buildTa4jStrategy(BarSeries series, int lookbackPeriod, double rrRatio) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema20 = new EMAIndicator(close, 20);
        EMAIndicator ema50 = new EMAIndicator(close, 50);

        Rule entryRule = new CrossedUpIndicatorRule(ema20, ema50);
        Rule exitRule = new CrossedDownIndicatorRule(ema20, ema50)
                .or(new StopLossRule(close, 2.0))   // Exemple : Stop Loss 2%
                .or(new StopGainRule(close, 3.0)) // Exemple : Take Profit 3%
                .or(new DynamicStopLossRule(series, lookbackPeriod, rrRatio));

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

    // Aligner le TP et SL au DynamicStopLossRule
    private TradeSignalDTO buildTradeFilterSignal(String direction, Num price, CandleDTO candle, List<CandleDTO> recentCandles, double rrRatio, String symbol) {
        double entry = price.doubleValue();
        double stopLoss;
        double takeProfit;

        if (direction.equals("BUY")) {
            double lowestLow = recentCandles.stream()
                    .mapToDouble(c -> c.getLow().doubleValue())
                    .min()
                    .orElse(entry * 0.99); // fallback
            stopLoss = lowestLow;
            takeProfit = entry + (entry - stopLoss) * rrRatio;
        } else {
            double highestHigh = recentCandles.stream()
                    .mapToDouble(c -> c.getHigh().doubleValue())
                    .max()
                    .orElse(entry * 1.01); // fallback
            stopLoss = highestHigh;
            takeProfit = entry - (stopLoss - entry) * rrRatio;
        }

        return TradeSignalDTO.builder()
                .tradeType(direction.equals("BUY") ? TradeSignalDTO.TradeType.LONG : TradeSignalDTO.TradeType.SHORT)
                .entryPrice(entry)
                .stopLoss(stopLoss)
                .takeProfit(takeProfit)
                .confidenceScore(1.0)
                .timestamp(candle.getDate())
                .symbol(symbol)
                .build();
    }
}
