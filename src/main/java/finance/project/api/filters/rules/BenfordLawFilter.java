package finance.project.api.filters.rules;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BenfordLawFilter {

    // Distribution théorique des premiers chiffres selon la loi de Benford
    private static final double[] BENFORD_DISTRIBUTION = {
            0.0, 0.301, 0.176, 0.125, 0.097, 0.079, 0.067, 0.058, 0.051, 0.046
    };

    /**
     * Calcule le score de conformité à la loi de Benford sur une liste de variations de prix.
     *
     * @param priceChanges Liste des variations de prix.
     * @return Score d'écart (0 = parfaitement conforme, > 0 = anomalie détectée).
     */
    public double calculateBenfordScore(List<Double> priceChanges) {
        if (priceChanges.isEmpty()) return 1.0; // Pas de données, donc suspicion

        Map<Integer, Integer> digitCount = new HashMap<>();

        // Compter les premiers chiffres significatifs
        for (double change : priceChanges) {
            int leadingDigit = getFirstSignificantDigit(change);
            digitCount.put(leadingDigit, digitCount.getOrDefault(leadingDigit, 0) + 1);
        }

        // Convertir en distribution relative
        int total = priceChanges.size();
        double score = 0.0;

        for (int i = 1; i <= 9; i++) {
            double observedFrequency = digitCount.getOrDefault(i, 0) / (double) total;
            double expectedFrequency = BENFORD_DISTRIBUTION[i];

            // Mesurer l'écart entre les distributions (MSE)
            score += Math.pow(observedFrequency - expectedFrequency, 2);
        }

        return score; // Plus le score est élevé, plus l'anomalie est grande
    }

    /**
     * Récupère le premier chiffre significatif d'un nombre.
     */
    private int getFirstSignificantDigit(double number) {
        number = Math.abs(number);
        while (number >= 10) number /= 10;
        while (number < 1 && number > 0) number *= 10;
        return (int) number;
    }
}
