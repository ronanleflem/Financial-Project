package finance.project.api.filters.rules;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.CandleDTO;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vérification de la stabilité du marché pour valider les modèles quantitatifs.
 */
@Service
public class StationarityFilter implements Filter {

    public double test(double[] timeSeries, int maxLag) {
        int n = timeSeries.length;

        // Calcul des différences premières
        double[] diffSeries = new double[n - 1];
        for (int i = 1; i < n; i++) {
            diffSeries[i - 1] = timeSeries[i] - timeSeries[i - 1];
        }

        // Construction de la matrice de régression
        double[][] regressionMatrix = new double[n - maxLag - 1][maxLag + 1];
        double[] y = new double[n - maxLag - 1];

        for (int i = maxLag; i < n - 1; i++) {
            y[i - maxLag] = diffSeries[i];
            regressionMatrix[i - maxLag][0] = timeSeries[i];
            for (int j = 1; j <= maxLag; j++) {
                regressionMatrix[i - maxLag][j] = diffSeries[i - j];
            }
        }

        // Régression linéaire multiple
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(y, regressionMatrix);
        double[] beta = regression.estimateRegressionParameters();
        double testStatistic = beta[0] / regression.estimateRegressionParametersStandardErrors()[0];

        return testStatistic;
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
        if (candles.size() < 20) return 0;
        double[] values = candles.stream().mapToDouble(c -> c.getClose().doubleValue()).toArray();
        double stat = test(values, 5);
        return Math.abs(stat) > 2 ? 1 : 0;
    }
}
