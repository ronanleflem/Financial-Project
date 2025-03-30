package finance.project.api.bootstrap;

import finance.project.api.strategies.volume.EmaVolumeStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BacktestExecutor;
import org.ta4j.core.reports.PerformanceReport;

import java.sql.SQLException;

public class BacktestRunner {
    public static void main(String[] args) throws SQLException {
        TimeSeries series = MySQLHistoricalLoader.loadFromDatabase();
        Strategy strategy = EmaVolumeStrategy.buildStrategy(series);

        TradingRecord tradingRecord = new BacktestExecutor()
                .withInitialBalance(10000)  // 💰 Capital initial 10 000$
                .withRiskPerTrade(2)        // 📉 2% de risque par trade
                .withSlippage(0.0001)       // 🔄 Slippage de 1 pip
                .withSpread(0.0002)         // 📊 Spread de 2 pips
                .backtest(series, strategy);

        // 🔥 Analyse des résultats
        PerformanceReport report = new PerformanceAnalyzer().analyze(tradingRecord);
        System.out.println(report);
    }
}