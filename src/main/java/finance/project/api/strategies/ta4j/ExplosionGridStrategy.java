package finance.project.api.strategies.ta4j;

import finance.project.api.model.*;
import finance.project.api.services.CandleCacheManager;
import finance.project.api.services.TA4JService;
import finance.project.api.services.TradeFilterService;
import finance.project.api.utils.MarketConventionUtils;
import finance.project.api.utils.StrategyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExplosionGridStrategy {

    private final TA4JService ta4jService;
    private final CandleCacheManager candleCacheManager;
    private final TradeFilterService tradeFilterService;

    @Autowired
    public ExplosionGridStrategy(TA4JService ta4jService,
                                 CandleCacheManager candleCacheManager,
                                 TradeFilterService tradeFilterService) {
        this.ta4jService = ta4jService;
        this.candleCacheManager = candleCacheManager;
        this.tradeFilterService = tradeFilterService;
    }

    public StrategyResult execute(String symbol, String timeframe, int period,
                                  double explosionPct, double stepPct) {
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
        EMAIndicator ema20 = new EMAIndicator(close, 20);
        RSIIndicator rsi14 = new RSIIndicator(close, 14);

        List<TradeSignalDTO> signals = new ArrayList<>();
        List<CompletedTradeDTO> completedTrades = new ArrayList<>();

        TradeSignalDTO longEntry = null;
        TradeSignalDTO shortEntry = null;
        double lastEntryPrice = 0.0;
        boolean pairOpen = false;

        for (int i = 1; i < series.getBarCount(); i++) {
            Num priceNum = close.getValue(i);
            double price = priceNum.doubleValue();
            double prev = close.getValue(i - 1).doubleValue();
            CandleDTO candle = candles.get(i);

            if (!pairOpen) {
                if (price >= prev * (1 + explosionPct / 100.0)) {
                    longEntry = buildSimpleSignal("BUY", price, candle, symbol);
                    shortEntry = buildSimpleSignal("SELL", price, candle, symbol);
                    signals.add(longEntry);
                    signals.add(shortEntry);
                    pairOpen = true;
                    lastEntryPrice = price;
                }
            } else {
                if (price >= lastEntryPrice * (1 + stepPct / 100.0)) {
                    TradeExitDTO exitLong = new TradeExitDTO(price, candle.getDate());
                    TradeExitDTO exitShort = new TradeExitDTO(price, candle.getDate());
                    if (longEntry != null) completedTrades.add(new CompletedTradeDTO(longEntry, exitLong));
                    if (shortEntry != null) completedTrades.add(new CompletedTradeDTO(shortEntry, exitShort));

                    longEntry = buildSimpleSignal("BUY", price, candle, symbol);
                    shortEntry = buildSimpleSignal("SELL", price, candle, symbol);
                    signals.add(longEntry);
                    signals.add(shortEntry);
                    lastEntryPrice = price;
                } else if (price < ema20.getValue(i).doubleValue()
                        && rsi14.getValue(i).doubleValue() < 50) {
                    if (longEntry != null) {
                        TradeExitDTO exitLong = new TradeExitDTO(price, candle.getDate());
                        completedTrades.add(new CompletedTradeDTO(longEntry, exitLong));
                        longEntry = null;
                    }
                }
            }
        }

        if (longEntry != null) {
            Num lastNum = close.getValue(series.getEndIndex());
            CandleDTO candle = candles.get(series.getEndIndex());
            completedTrades.add(new CompletedTradeDTO(longEntry,
                    new TradeExitDTO(lastNum.doubleValue(), candle.getDate())));
        }
        if (shortEntry != null) {
            Num lastNum = close.getValue(series.getEndIndex());
            CandleDTO candle = candles.get(series.getEndIndex());
            completedTrades.add(new CompletedTradeDTO(shortEntry,
                    new TradeExitDTO(lastNum.doubleValue(), candle.getDate())));
        }

        Map<String, Double> performance = computeManualPerformance(completedTrades);
        return new StrategyResult(signals, completedTrades, performance, "ExplosionGrid");
    }

    private TradeSignalDTO buildSimpleSignal(String direction, double price, CandleDTO candle, String symbol) {
        return TradeSignalDTO.builder()
                .tradeType(direction.equals("BUY") ? TradeSignalDTO.TradeType.LONG : TradeSignalDTO.TradeType.SHORT)
                .entryPrice(price)
                .stopLoss(0)
                .takeProfit(0)
                .confidenceScore(1.0)
                .timestamp(candle.getDate())
                .symbol(symbol)
                .build();
    }

    public Map<String, Double> computeManualPerformance(List<CompletedTradeDTO> trades) {
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

        double commissionPerTrade = 0.5; // en pips

        for (CompletedTradeDTO trade : trades) {
            TradeSignalDTO entry = trade.getEntrySignal();
            TradeExitDTO exit = trade.getExitSignal();
            if (entry == null || exit == null) continue;

            String symbol = entry.getSymbol();
            double pipFactor = MarketConventionUtils.getPipFactor(symbol);

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

            double netPips = MarketConventionUtils.computeNetPips(rawPips, symbol, commissionPerTrade);
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
