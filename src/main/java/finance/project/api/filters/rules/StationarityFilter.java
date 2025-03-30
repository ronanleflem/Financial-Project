package finance.project.api.filters.rules;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.List;

/**
 * Vérification de la stabilité du marché pour valider les modèles quantitatifs.
 */
public class StationarityFilter {

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

}
