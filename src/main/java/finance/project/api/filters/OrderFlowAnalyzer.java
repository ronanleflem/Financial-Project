package finance.project.api.filters;


import finance.project.api.entities.PointOfInterest;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * ✅ Associer ça avec un algorithme de clustering pour voir où se regroupe le volume
 * ✅ Analyser l’absorption des ordres pour détecter un potentiel retournement
 */
@Service
public class OrderFlowAnalyzer {

    private static final double DELTA_THRESHOLD = 1000; // Seuil pour considérer un déséquilibre fort

    /**
     * Vérifie si un niveau institutionnel est soutenu par du volume d'achat/vente.
     *
     * @param price Prix actuel
     * @param keyLevels Liste des niveaux institutionnels détectés
     * @param buyVolumes Liste des volumes acheteurs sur chaque niveau
     * @param sellVolumes Liste des volumes vendeurs sur chaque niveau
     * @return Score de validation des niveaux (0 = non valide, 1-2 = modéré, 3+ = fort)
     */
    public int validateZonesWithOrderFlow(double price, List<PointOfInterest> keyLevels, List<Double> buyVolumes, List<Double> sellVolumes) {
        int validationScore = 0;

        for (int i = 0; i < keyLevels.size(); i++) {
            double level = (keyLevels.get(i).getHigh()+keyLevels.get(i).getLow()) /2;
            double deltaVolume = buyVolumes.get(i) - sellVolumes.get(i);

            if (Math.abs(price - level) <= 0.0015) { // Zone proche
                if (Math.abs(deltaVolume) > DELTA_THRESHOLD) {
                    validationScore++; // Niveau validé par un flux d'ordres fort
                }
            }
        }

        return validationScore;
    }
}
