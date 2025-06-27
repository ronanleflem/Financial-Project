package finance.project.api.filters.rules;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.CandleDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class StatisticalArbitrageFilter implements Filter {

    public List<Double> calculateDailyReturns(List<Double> prices) {
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            double dailyReturn = (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1);
            returns.add(dailyReturn);
        }
        return returns;
    }

    public double calculateOmegaRatio(List<Double> returns, double threshold) {
        double gainSum = 0.0;
        double lossSum = 0.0;
        for (double r : returns) {
            if (r > threshold) {
                gainSum += r;
            } else {
                lossSum += Math.abs(r);
            }
        }
        return gainSum / lossSum;
    }

    public double calculateInformationRatio(List<Double> returns, double benchmarkReturn) {
        double excessReturnSum = 0.0;
        double squaredDeviationSum = 0.0;
        for (double r : returns) {
            double excessReturn = r - benchmarkReturn;
            excessReturnSum += excessReturn;
            squaredDeviationSum += excessReturn * excessReturn;
        }
        double meanExcessReturn = excessReturnSum / returns.size();
        double trackingError = Math.sqrt(squaredDeviationSum / (returns.size() - 1));
        return meanExcessReturn / trackingError;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest) {
        return 1;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, Symbol symbol, String timeframe, int period) {
        return 1;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, String symbol, String timeframe, int period) {
        return 1;
    }
    
    @Override
    public int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) {
        if (candles.size() < 30) return 0;
        List<Double> prices = candles.stream().map(c -> c.getClose().doubleValue()).toList();
        List<Double> returns = calculateDailyReturns(prices);
        double omega = calculateOmegaRatio(returns, 0);
        return omega > 1 ? 1 : 0;
    }
}
