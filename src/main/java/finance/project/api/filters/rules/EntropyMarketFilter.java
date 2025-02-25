package finance.project.api.filters.rules;


import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EntropyMarketFilter {

    /**
     * Calcule l'entropie de Shannon des variations de prix.
     *
     * @param priceChanges Liste des variations de prix successives
     * @return Valeur d'entropie (0 = marché structuré, proche de 1 = marché chaotique)
     */
    public double calculateMarketEntropy(List<Double> priceChanges) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();

        for (double change : priceChanges) {
            int bucket = (int) Math.round(change * 1000); // Regroupement des valeurs
            frequencyMap.put(bucket, frequencyMap.getOrDefault(bucket, 0) + 1);
        }

        double entropy = 0.0;
        int totalCount = priceChanges.size();

        for (int count : frequencyMap.values()) {
            double probability = (double) count / totalCount;
            entropy -= probability * Math.log(probability) / Math.log(2);
        }

        return entropy / Math.log(totalCount);
    }
}
