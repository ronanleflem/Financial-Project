package finance.project.api.filters.rules;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MarketManipulationFilter {

    private final EntropyMarketFilter entropyMarketFilter;
    private final FractalAnalysisFilter fractalAnalysisFilter;
    private final VolatilityFilter volatilityFilter;

    public MarketManipulationFilter(EntropyMarketFilter entropyMarketFilter, FractalAnalysisFilter fractalAnalysisFilter, VolatilityFilter volatilityFilter) {
        this.entropyMarketFilter = entropyMarketFilter;
        this.fractalAnalysisFilter = fractalAnalysisFilter;
        this.volatilityFilter = volatilityFilter;
    }

    /**
     * Détecte les zones de manipulation basées sur Entropie + Kurtosis.
     *
     * @param priceChanges Liste des variations de prix successives
     * @return Score de manipulation (0 = normal, 1 = douteux, 2+ = manipulation forte)
     */
    public int detectManipulationZone(List<Double> priceChanges) {
        if (priceChanges.size() < 20) return 0; // Pas assez de données

        double entropy = volatilityFilter.calculateMarketEntropy(priceChanges);
        double kurtosis = fractalAnalysisFilter.calculateKurtosis(priceChanges);

        int manipulationScore = 0;

        // Entropie élevée (marché chaotique)
        if (entropy > 0.7) {
            manipulationScore++;
        }

        // Kurtosis élevée (pics extrêmes fréquents)
        if (kurtosis > 3) {
            manipulationScore++;
        }

        // Zone critique (manipulation très probable)
        if (entropy > 0.8 && kurtosis > 4) {
            manipulationScore++;
        }

        return manipulationScore;
    }
}
