package finance.project.api.filters.rules;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 📊 Donchian Channels en Trading Institutionnel
 * Utilisation	Pourquoi c’est utile pour les institutionnels ?
 * Breakout Trading	Détecter les cassures de range et entrer sur des tendances fortes.
 * Repérage des zones de liquidité	Les institutions traquent les niveaux extrêmes pour piéger les retailers.
 * Filtrer les fausses cassures	Vérifier si le breakout est soutenu par du volume.
 * Détection des ranges et consolidation	Identifier les zones où le marché accumule avant un mouvement.
 *
 * 📌 Institutionnels & Donchian Channels
 *
 *     Ils utilisent souvent DC20 / DC50 / DC100 pour repérer les extrêmes de marché.
 *     Ils recherchent les manipulations autour du haut/bas du canal pour liquider les stops.
 *     Ils combinent Order Flow + Donchian Channels pour voir si un breakout est légitime ou piégeux.
 */
@Service
public class DonchianChannelsFilter implements Filter {

    /**
     * Calcule les bandes de Donchian pour une période donnée.
     *
     * @param highs Liste des plus hauts (High) sur la période.
     * @param lows Liste des plus bas (Low) sur la période.
     * @return Tableau [Upper Band, Lower Band, Middle Band]
     */
    public double[] calculateDonchianBands(List<Double> highs, List<Double> lows) {
        if (highs.size() != lows.size() || highs.isEmpty()) {
            throw new IllegalArgumentException("Données invalides pour calculer les Donchian Channels");
        }

        double upperBand = highs.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        double lowerBand = lows.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        double middleBand = (upperBand + lowerBand) / 2.0;

        return new double[]{upperBand, lowerBand, middleBand};
    }

    /**
     * 📌 Utilisation des Donchian Channels en Stratégie
     * 1️⃣ Breakout Confirmé (Avec Volume)
     *
     * 💡 Si le prix casse le haut du canal et que l'Order Flow montre un delta volume positif fort, alors c'est une cassure valide.
     * 📌 Plan d’action → Entrer après un retest du haut du canal.
     * 2️⃣ Fake Breakout (Manipulation)
     *
     * 💡 Si le prix casse le haut mais que l’Order Flow montre un delta faible ou négatif, c'est un bull trap.
     * 📌 Plan d’action → Shorter le faux breakout après confirmation.
     * 3️⃣ Rebond sur le bas du canal
     *
     * 💡 Si le prix touche le bas du canal et que le delta volume acheteur est élevé, c'est une accumulation.
     * 📌 Plan d’action → Entrer long avec stop sous le bas du canal.
     *
     * @param price
     * @param upperBand
     * @param buyVolumes
     * @param sellVolumes
     * @return
     */
    public boolean isValidBreakout(double price, double upperBand, List<Double> buyVolumes, List<Double> sellVolumes) {
        double deltaVolume = buyVolumes.stream().mapToDouble(Double::doubleValue).sum() -
                sellVolumes.stream().mapToDouble(Double::doubleValue).sum();

        return price > upperBand && deltaVolume > 1000; // Seuil de validation
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest) {
        return 1;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, Symbol symbol, String timeframe, int period) {
        return 1;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, String symbol, String timeframe, int period) {
        return 1;
    }
}
