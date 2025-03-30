package finance.project.api.filters.rules;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class StatisticalArbitrageFilter {

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


}
