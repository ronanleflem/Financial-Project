package finance.project.api.utils;

public class Kurtosis {

    /**
     * Calcule l'excess kurtosis d'une série de données.
     *
     * @param values tableau de doubles représentant la série
     * @return kurtosis (excess kurtosis : 0 = distribution normale)
     */
    public double evaluate(double[] values) {
        int n = values.length;
        if (n < 4) return 0.0; // Minimum 4 points requis pour kurtosis corrigé

        double mean = mean(values);
        double stdDev = standardDeviation(values, mean);

        if (stdDev == 0) return 0.0; // Pas de dispersion = pas de kurtosis pertinent

        double kurtSum = 0.0;
        for (double v : values) {
            kurtSum += Math.pow((v - mean) / stdDev, 4);
        }

        double numerator = (n * (n + 1)) * kurtSum;
        double denominator = (n - 1) * (n - 2) * (n - 3);

        double excessKurtosis = (numerator / denominator) - (3.0 * Math.pow(n - 1, 2) / ((n - 2) * (n - 3)));

        return excessKurtosis;
    }

    private double mean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private double standardDeviation(double[] values, double mean) {
        double sum = 0.0;
        for (double v : values) {
            sum += Math.pow(v - mean, 2);
        }
        return Math.sqrt(sum / (values.length - 1));
    }
}
