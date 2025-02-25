package finance.project.api.filters.rules;

import java.util.List;

public class HighTimeframeZoneFilter {

    private static final double PROXIMITY_THRESHOLD = 0.0015; // ≈ 15 pips pour EUR/USD

    /**
     * Vérifie si le prix actuel est proche d'une zone institutionnelle clé.
     *
     * @param price Prix actuel
     * @param keyLevels Liste des niveaux institutionnels détectés
     * @return Score de confluence basé sur le nombre de zones proches
     */
    public int checkInstitutionalConfluence(double price, List<Double> keyLevels) {
        int confluenceScore = 0;

        for (double level : keyLevels) {
            if (Math.abs(price - level) <= PROXIMITY_THRESHOLD) {
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
    public int checkInstitutionalConfluenceWithOrderFlow(double price, List<Double> keyLevels, List<Double> buyVolumes, List<Double> sellVolumes) {
        int confluenceScore = 0;

        // Vérification de proximité des niveaux institutionnels
        for (double level : keyLevels) {
            if (Math.abs(price - level) <= 0.0015) {
                confluenceScore++;
            }
        }

        // Ajout du score Order Flow
        int orderFlowScore = orderFlowAnalyzer.validateZonesWithOrderFlow(price, keyLevels, buyVolumes, sellVolumes);

        return confluenceScore + orderFlowScore; // Score total de validation
    }
}
