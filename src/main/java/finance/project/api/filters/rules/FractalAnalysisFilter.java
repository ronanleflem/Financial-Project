package finance.project.api.filters.rules;


import finance.project.api.utils.Kurtosis;
import finance.project.api.utils.Skewness;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FractalAnalysisFilter {
    private final Kurtosis kurtosis = new Kurtosis();
    private final Skewness skewness = new Skewness();
    /**
     * Calcule le Ratio de Hurst pour mesurer le comportement fractal du marché.
     *
     * @param priceChanges Liste des variations de prix
     * @return Valeur du Ratio de Hurst (0.5 = marché aléatoire, >0.5 = tendance, <0.5 = réversion)
     */
    public double calculateHurstExponent(List<Double> priceChanges) {
        int N = priceChanges.size();
        if (N < 20) {
            return 0.5; // Pas assez de données, on retourne la valeur d'un marché aléatoire.
        }

        // Moyenne des variations de prix
        double mean = priceChanges.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // Création de la série cumulative (X(t) - moyenne)
        double[] cumulativeSeries = new double[N];
        double cumulativeDeviation = 0.0;
        for (int i = 0; i < N; i++) {
            cumulativeDeviation += priceChanges.get(i) - mean;
            cumulativeSeries[i] = cumulativeDeviation;
        }

        // Calcul de la plage R = max - min de la série cumulative
        double maxCumulative = cumulativeSeries[0];
        double minCumulative = cumulativeSeries[0];
        for (int i = 1; i < N; i++) {
            if (cumulativeSeries[i] > maxCumulative) {
                maxCumulative = cumulativeSeries[i];
            }
            if (cumulativeSeries[i] < minCumulative) {
                minCumulative = cumulativeSeries[i];
            }
        }
        double R = maxCumulative - minCumulative;

        // Calcul de l'écart type S des variations de prix
        double sumSquaredDeviations = 0.0;
        for (int i = 0; i < N; i++) {
            sumSquaredDeviations += Math.pow(priceChanges.get(i) - mean, 2);
        }
        double S = Math.sqrt(sumSquaredDeviations / N);

        // Évitons la division par zéro
        if (S == 0) {
            return 0.5; // Pas de volatilité => comportement aléatoire
        }

        // Calcul du ratio R/S
        double rescaledRange = R / S;

        // Calcul du Hurst exponent : log(R/S) / log(N)
        double hurstExponent = Math.log(rescaledRange) / Math.log(N);

        // Assurons-nous que le résultat est dans [0, 1]
        hurstExponent = Math.max(0.0, Math.min(hurstExponent, 1.0));

        return hurstExponent;
    }

    /**
     * Calcule la Kurtosis des rendements du marché.
     *
     * @param returns Liste des rendements successifs
     * @return Kurtosis (Valeur élevée = pics extrêmes fréquents)
     */
    public double calculateKurtosis(List<Double> returns) {
        if (returns.size() < 20) return 0.0;
        double[] data = returns.stream().mapToDouble(Double::doubleValue).toArray();
        return kurtosis.evaluate(data);
    }

    /**
     * Calcule le Skewness des rendements du marché.
     *
     * @param returns Liste des rendements successifs
     * @return Skewness (Valeur > 0 = biais haussier, < 0 = biais baissier)
     */
    public double calculateSkewness(List<Double> returns) {
        if (returns.size() < 20) return 0.0;
        double[] data = returns.stream().mapToDouble(Double::doubleValue).toArray();
        return skewness.evaluate(data);
    }
}
