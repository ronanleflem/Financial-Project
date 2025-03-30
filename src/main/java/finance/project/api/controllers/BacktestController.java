package finance.project.api.controllers;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.services.CandleService;
import finance.project.api.services.SymbolService;
import finance.project.api.services.TA4JService;
import finance.project.api.services.VolumeBasedRolloverService;
import finance.project.api.strategies.volume.EmaVolumeStrategy;
import jdk.jfr.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

    @Autowired
    public BacktestController(TA4JService ta4JService, VolumeBasedRolloverService volumeBasedRolloverService, EmaVolumeStrategy emaVolumeStrategy, SymbolService symbolService, CandleService candleService) {
        this.ta4JService = ta4JService;
        this.volumeBasedRolloverService = volumeBasedRolloverService;
        this.emaVolumeStrategy = emaVolumeStrategy;
        this.symbolService = symbolService;
        this.candleService = candleService;
    }

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
    }
}
