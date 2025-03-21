package finance.project.api.utils;

public class Skewness {

    /**
     * Calcule le skewness d'une série de données.
     *
     * @param values tableau de doubles représentant la série
     * @return skewness
     */
    public double evaluate(double[] values) {
        int n = values.length;
        if (n < 3) return 0.0; // Besoin d'au moins 3 points pour la formule correcte

        double mean = mean(values);
        double stdDev = standardDeviation(values, mean);

        if (stdDev == 0) return 0.0; // Pas de dispersion = pas d'asymétrie

        double skewSum = 0.0;
        for (double v : values) {
            skewSum += Math.pow((v - mean) / stdDev, 3);
        }

        // Correction de biais sur petits échantillons
        return (n * skewSum) / ((n - 1) * (n - 2));
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
        return Math.sqrt(sum / (values.length - 1)); // écart-type échantillon
    }
}
