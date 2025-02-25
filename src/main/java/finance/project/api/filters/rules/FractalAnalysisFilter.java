package finance.project.api.filters.rules;


import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FractalAnalysisFilter {

    /**
     * Calcule le Ratio de Hurst pour mesurer le comportement fractal du marché.
     *
     * @param priceChanges Liste des variations de prix
     * @return Valeur du Ratio de Hurst (0.5 = marché aléatoire, >0.5 = tendance, <0.5 = réversion)
     */
    public double calculateHurstExponent(List<Double> priceChanges) {
        int N = priceChanges.size();
        if (N < 20) return 0.5; // Pas assez de données

        double mean = priceChanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double cumulativeDeviation = 0;
        double sumSquaredDeviations = 0;

        double[] cumulativeSeries = new double[N];
        for (int i = 0; i < N; i++) {
            cumulativeDeviation += priceChanges.get(i) - mean;
            cumulativeSeries[i] = cumulativeDeviation;
            sumSquaredDeviations += Math.pow(priceChanges.get(i) - mean, 2);
        }

        double R = Math.max(cumulativeSeries) - Math.min(cumulativeSeries);
        double S = Math.sqrt(sumSquaredDeviations / N);

        return (S == 0) ? 0.5 : Math.log(R / S) / Math.log(N);
    }
    /**
     * Calcule la Kurtosis des rendements du marché.
     *
     * @param returns Liste des rendements successifs
     * @return Kurtosis (Valeur élevée = pics extrêmes fréquents)
     */
    public double calculateKurtosis(List<Double> returns) {
        if (returns.size() < 20) return 0.0; // Besoin d'un minimum de données

        Kurtosis kurtosis = new Kurtosis();
        return kurtosis.evaluate(returns.stream().mapToDouble(Double::doubleValue).toArray());
    }

    /**
     * Calcule le Skewness des rendements du marché.
     *
     * @param returns Liste des rendements successifs
     * @return Skewness (Valeur > 0 = biais haussier, < 0 = biais baissier)
     */
    public double calculateSkewness(List<Double> returns) {
        if (returns.size() < 20) return 0.0;

        Skewness skewness = new Skewness();
        return skewness.evaluate(returns.stream().mapToDouble(Double::doubleValue).toArray());
    }
}
