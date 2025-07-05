package finance.project.api.strategies.ta4j;

import finance.project.api.filters.ta4j.AdxAboveRule;
import finance.project.api.filters.ta4j.AtrRisingRule;
import finance.project.api.filters.ta4j.LinearRegressionMacdCrossRule;
import finance.project.api.filters.ta4j.VolumeAboveAverageRule;
import finance.project.api.model.*;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.OverIndicatorRule;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MacdPredictionStrategy {

    private final TA4JService ta4jService;
    private final CandleCacheManager candleCacheManager;

    @Autowired
    public MacdPredictionStrategy(TA4JService ta4jService,
                                  CandleCacheManager candleCacheManager) {
        this.ta4jService = ta4jService;
        this.candleCacheManager = candleCacheManager;
    }

    public StrategyResult execute(String symbol, String timeframe, int period, double rrRatio) {
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

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        MACDIndicator macd = new MACDIndicator(close, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);
        EMAIndicator ema200 = new EMAIndicator(close, 200);
        ADXIndicator adx = new ADXIndicator(series, 14);
        ATRIndicator atr = new ATRIndicator(series, 14);
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator volumeSma = new SMAIndicator(volume, 20);

        Rule entryRule = new LinearRegressionMacdCrossRule(macd, macdSignal, 5, true);
        /*
                .and(new OverIndicatorRule(close, ema200))
                .and(new AdxAboveRule(adx, 20))
                .and(new VolumeAboveAverageRule(volume, 20))
                .and(new AtrRisingRule(atr,20));*/

        Rule exitRule = new LinearRegressionMacdCrossRule(macd, macdSignal, 5, false)
                .or(new DynamicStopLossRule(series, 20, rrRatio));

        Strategy strategy = new BaseStrategy("MacdPrediction", entryRule, exitRule);

        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord backtest = manager.run(strategy); // not used but ensures rules are valid

        TradingRecord recordlive = new BaseTradingRecord();
        List<TradeSignalDTO> signals = new ArrayList<>();
        List<CompletedTradeDTO> completedTrades = new ArrayList<>();
        TradeSignalDTO entrySignal = null;

        for (int i = 0; i < series.getBarCount(); i++) {
            CandleDTO candle = candles.get(i);
            Num price = close.getValue(i);

            if (strategy.shouldEnter(i) && recordlive.isClosed()) {
                recordlive.enter(i, price, series.getBar(i).getVolume());
                List<CandleDTO> recent = candles.subList(Math.max(0, i - 20), i);
                entrySignal = buildSignal("BUY", price, candle, recent, rrRatio, symbol);
                signals.add(entrySignal);
            } else if (strategy.shouldExit(i) && !recordlive.isClosed() && entrySignal != null) {
                recordlive.exit(i, price, series.getBar(i).getVolume());
                TradeExitDTO exit = new TradeExitDTO(price.doubleValue(), candle.getDate());
                completedTrades.add(new CompletedTradeDTO(entrySignal, exit));
                entrySignal = null;
            }
        }

        Map<String, Double> performance = computeManualPerformance(completedTrades);
        return new StrategyResult(signals, completedTrades, performance, "MacdPrediction");
    }

    private TradeSignalDTO buildSignal(String direction, Num price, CandleDTO candle,
                                       List<CandleDTO> recentCandles, double rrRatio, String symbol) {
        double entry = price.doubleValue();
        double stopLoss;
        double takeProfit;

        if (direction.equals("BUY")) {
            double lowestLow = recentCandles.stream()
                    .mapToDouble(c -> c.getLow().doubleValue())
                    .min()
                    .orElse(entry * 0.99);
            stopLoss = lowestLow;
            takeProfit = entry + (entry - stopLoss) * rrRatio;
        } else {
            double highestHigh = recentCandles.stream()
                    .mapToDouble(c -> c.getHigh().doubleValue())
                    .max()
                    .orElse(entry * 1.01);
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

    private Map<String, Double> computeManualPerformance(List<CompletedTradeDTO> trades) {
        double totalNetReturn = 0.0;
        double totalRawReturn = 0.0;
        int netWinCount = 0;
        int netLossCount = 0;
        int rawWinCount = 0;
        int rawLossCount = 0;
        double totalSL = 0.0;
        double totalTP = 0.0;
        int slCount = 0;
        int tpCount = 0;

        LocalDateTime startStrategy = null;
        LocalDateTime endStrategy = null;
        double totalRR = 0.0;
        int rrCount = 0;

        double commissionPerTrade = 0.5; // in pips

        for (CompletedTradeDTO trade : trades) {
            TradeSignalDTO entry = trade.getEntrySignal();
            TradeExitDTO exit = trade.getExitSignal();
            if (entry == null || exit == null) continue;

            String sym = entry.getSymbol();
            double pipFactor = MarketConventionUtils.getPipFactor(sym);

            LocalDateTime entryTime = entry.getTimestamp();
            LocalDateTime exitTime = exit.getTimestamp();
            if (startStrategy == null || entryTime.isBefore(startStrategy)) {
                startStrategy = entryTime;
            }
            if (endStrategy == null || exitTime.isAfter(endStrategy)) {
                endStrategy = exitTime;
            }

            double entryPrice = entry.getEntryPrice();
            double exitPrice = exit.getExitPrice();

            double rawPips = (entry.getTradeType() == TradeSignalDTO.TradeType.LONG)
                    ? exitPrice - entryPrice
                    : entryPrice - exitPrice;

            rawPips *= pipFactor;
            totalRawReturn += rawPips;

            if (rawPips > 0) rawWinCount++; else if (rawPips < 0) rawLossCount++;

            double netPips = MarketConventionUtils.computeNetPips(rawPips, sym, commissionPerTrade);
            totalNetReturn += netPips;
            if (netPips > 0) netWinCount++; else if (netPips < 0) netLossCount++;

            double slPips = Math.abs(entryPrice - entry.getStopLoss()) * pipFactor;
            double tpPips = Math.abs(entry.getTakeProfit() - entryPrice) * pipFactor;

            if (entry.getStopLoss() > 0) { totalSL += slPips; slCount++; }
            if (entry.getTakeProfit() > 0) { totalTP += tpPips; tpCount++; }
            if (slPips > 0) { totalRR += tpPips / slPips; rrCount++; }
        }

        int totalTrades = rawWinCount + rawLossCount;
        double averageRawTrade = totalTrades > 0 ? totalRawReturn / totalTrades : 0.0;
        double averageNetTrade = totalTrades > 0 ? totalNetReturn / totalTrades : 0.0;
        double averageSL = slCount > 0 ? totalSL / slCount : 0.0;
        double averageTP = tpCount > 0 ? totalTP / tpCount : 0.0;
        double rrMoyen = rrCount > 0 ? totalRR / rrCount : 0.0;

        Map<String, Double> performance = new HashMap<>();
        performance.put("totalReturn", totalRawReturn);
        performance.put("totalNetReturn", totalNetReturn);
        performance.put("winCount", (double) rawWinCount);
        performance.put("lossCount", (double) rawLossCount);
        performance.put("netWinCount", (double) netWinCount);
        performance.put("netLossCount", (double) netLossCount);
        performance.put("averageTrade", averageRawTrade);
        performance.put("averageNetTrade", averageNetTrade);
        performance.put("averageSL", averageSL);
        performance.put("averageTP", averageTP);
        performance.put("RRmoyen", rrMoyen);
        performance.put("startStrategy", startStrategy != null ? (double) startStrategy.toEpochSecond(ZoneOffset.UTC) : 0d);
        performance.put("endStrategy", endStrategy != null ? (double) endStrategy.toEpochSecond(ZoneOffset.UTC) : 0d);
        performance.put("maxDrawdown", 0d);
        return performance;
    }
}
