package finance.project.api.filters.rules;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MarketManipulationFilter {

    private final FractalAnalysisFilter fractalAnalysisFilter;
    private final VolatilityFilter volatilityFilter;

    public MarketManipulationFilter( FractalAnalysisFilter fractalAnalysisFilter, VolatilityFilter volatilityFilter) {
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
        //10000 Car EURUSD, à adapter
        double entropy = volatilityFilter.calculateMarketEntropy(priceChanges,10000);
        double kurtosis = fractalAnalysisFilter.calculateKurtosis(priceChanges);

        int manipulationScore = 0;

        // Entropie élevée (marché chaotique)NE
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
