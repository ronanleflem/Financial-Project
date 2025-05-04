package finance.project.api.controllers;

import finance.project.api.entities.MarketData;
import finance.project.api.entities.Performance;
import finance.project.api.entities.Trade;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.model.TradeSignalTa4jDTO;
import finance.project.api.services.*;
import finance.project.api.strategies.StrategyManager;
import finance.project.api.strategies.volume.EmaVolumeStrategy;
import finance.project.api.utils.StrategyResult;
import jdk.jfr.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BacktestExecutor;
import org.ta4j.core.reports.PerformanceReport;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class BacktestController {
    private final TA4JService ta4JService;
    private final VolumeBasedRolloverService volumeBasedRolloverService;
    private final EmaVolumeStrategy emaVolumeStrategy;
    private final SymbolService symbolService;
    private final CandleService candleService;
    private final StrategyManager strategyManager;

    @Autowired
    private CandleCacheManager candleCacheManager;

    @Autowired
    public BacktestController(TA4JService ta4JService, VolumeBasedRolloverService volumeBasedRolloverService, EmaVolumeStrategy emaVolumeStrategy, SymbolService symbolService, CandleService candleService, StrategyManager strategyManager, MarketDataService marketDataService) {
        this.ta4JService = ta4JService;
        this.volumeBasedRolloverService = volumeBasedRolloverService;
        this.emaVolumeStrategy = emaVolumeStrategy;
        this.symbolService = symbolService;
        this.candleService = candleService;
        this.strategyManager = strategyManager;
    }
    @GetMapping("/run-strategy")
    public ResponseEntity<String> runStrategy(@RequestParam String symbol,
                                              @RequestParam String timeframe,
                                              @RequestParam int period) {
        //MarketData marketData = marketDataService.loadMarketData(symbol, timeframe, period); // Récupère les candles
        candleCacheManager.preload(symbol, timeframe, period);
        List<TradeSignalDTO> trades = strategyManager.runStrategies(symbol, timeframe, period);
        return ResponseEntity.ok("Stratégies exécutées sur " + symbol + " " + timeframe);
    }

    @GetMapping("/run-strategy-ta4j")
    public ResponseEntity<String> runStrategyTa4j(@RequestParam String symbol,
                                              @RequestParam String timeframe,
                                              @RequestParam int period) {
        //MarketData marketData = marketDataService.loadMarketData(symbol, timeframe, period); // Récupère les candles
        candleCacheManager.preload(symbol, timeframe, period);
        List<TradeSignalDTO> trades = strategyManager.runStrategies(symbol, timeframe, period);
        return ResponseEntity.ok("Stratégies exécutées sur " + symbol + " " + timeframe);
    }

    @GetMapping("/trend-following")
    public ResponseEntity<StrategyResult> runTrendFollowingBacktest(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam(defaultValue = "1000") int period) {

        // ⚠️ Important : on remplit le cache d'abord
        candleCacheManager.preload(symbol, timeframe, period);

        // 🧠 Exécute la stratégie TrendFollowing avec TA4J
        StrategyResult result = strategyManager.runTrendFollowing(symbol, timeframe, period);

        String strategyName = "TrendFollowing"; // ou dynamiquement via paramètre

        // ✅ Sauvegarder les trades
        List<Trade> trades = result.getSignals().stream()
                .map(signal -> Trade.builder()
                        .strategyName(strategyName)
                        .tradeType(signal.getTradeType())
                        .entryPrice(signal.getEntryPrice())
                        .stopLoss(signal.getStopLoss())
                        .takeProfit(signal.getTakeProfit())
                        .confidenceScore(signal.getConfidenceScore())
                        .timestamp(signal.getTimestamp())
                        .build())
                .toList();
        tradeRepository.saveAll(trades);

        // ✅ Sauvegarder les performances
        List<Performance> performances = result.getPerformance().entrySet().stream()
                .map(entry -> Performance.builder()
                        .strategyName(strategyName)
                        .metric(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
        performanceRepository.saveAll(performances);

        return ResponseEntity.ok(result);
    }
    /*
    @GetMapping
    public String backtest(@RequestParam String symbol, @RequestParam String timeframe,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<CandleDTO> candles;
        if(timeframe.equals("1min")){

            candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolumeOld(startDate, endDate, 2);

        }
        else {
            SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
            System.out.println(symbolDTO);

            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        }
        BarSeries series = ta4JService.convertToTimeSeries(candles,timeframe);
        Strategy strategy = emaVolumeStrategy.buildStrategy(series);

        TradingRecord tradingRecord = new BacktestExecutor(series)
                .withInitialBalance(10000)  // 💰 Capital initial 10 000$
                .withRiskPerTrade(2)        // 📉 2% de risque par trade
                .withSlippage(0.0001)       // 🔄 Slippage de 1 pip
                .withSpread(0.0002)         // 📊 Spread de 2 pips
                .backtest(series, strategy);

        // 🔥 Analyse des résultats
        PerformanceReport report = new PerformanceReport().a.analyze(tradingRecord);
        System.out.println(report);
    }*/
}
