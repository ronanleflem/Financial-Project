package finance.project.api.filters.rules;

import finance.project.api.entities.PointOfInterest;
import finance.project.api.filters.OrderFlowAnalyzer;
import finance.project.api.services.OrderFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HighTimeframeZoneFilter {

    private static final double PROXIMITY_THRESHOLD = 0.0015; // ≈ 15 pips pour EUR/USD

    private final OrderFlowAnalyzer orderFlowAnalyzer;

    /**
     * Vérifie si le prix actuel est proche d'une zone institutionnelle clé.
     *
     * @param price Prix actuel
     * @param keyLevels Liste des niveaux institutionnels détectés
     * @return Score de confluence basé sur le nombre de zones proches
     */
    public int checkInstitutionalConfluence(double price, List<PointOfInterest> keyLevels) {
        int confluenceScore = 0;

        for (PointOfInterest level : keyLevels) {
            if (Math.abs(price - (level.getHigh()+level.getLow()/2) ) <= PROXIMITY_THRESHOLD) {
                confluenceScore++;
            }
        }

        return confluenceScore;
    }

    /**
     * Vérifie si le prix est proche d'une zone institutionnelle et valide avec l'Order Flow.
     *
     * @param price Prix actuel
     * @param keyLevels Liste des niveaux institutionnels détectés
     * @param buyVolumes Liste des volumes acheteurs par niveau
     * @param sellVolumes Liste des volumes vendeurs par niveau
     * @return Score pondéré basé sur la confluence entre zone et Order Flow
     */
    public int checkInstitutionalConfluenceWithOrderFlow(double price, List<PointOfInterest> keyLevels, List<Double> buyVolumes, List<Double> sellVolumes) {
        int confluenceScore = 0;

        // Vérification de proximité des niveaux institutionnels
        for (PointOfInterest level : keyLevels) {
            if (Math.abs(price - (level.getHigh()+level.getLow()/2)) <= 0.0015) {
                confluenceScore++;
            }
        }

        // Ajout du score Order Flow
        int orderFlowScore = orderFlowAnalyzer.validateZonesWithOrderFlow(price, keyLevels, buyVolumes, sellVolumes);

        return confluenceScore + orderFlowScore; // Score total de validation
    }
}
