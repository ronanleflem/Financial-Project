package finance.project.api.filters.rules;


import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 📌 Objectif : LiquidityFilter avec Chaikin Money Flow (CMF)
 *
 * Le Chaikin Money Flow (CMF) mesure la pression acheteuse ou vendeuse en fonction du volume et de la position du prix dans la bougie. Il permet d’identifier :
 *
 * ✅ Accumulation institutionnelle → Beaucoup d’achats avec du volume élevé.
 * ✅ Distribution institutionnelle → Beaucoup de ventes avec du volume élevé.
 * ✅ Périodes de forte ou faible liquidité.
 *
 * 📌 Utilisation Stratégique
 *
 * 📌 1️⃣ Détecter l’accumulation institutionnelle
 * ✅ Si CMF > 0.3 et le prix est en range → Accumulation des institutionnels avant un breakout haussier.
 *
 * 📌 2️⃣ Filtrer les faux breakouts
 * ⚠ Si CMF est négatif lors d’un breakout haussier → Probable bull trap, éviter le long.
 *
 * 📌 3️⃣ Confirmer une tendance avec la liquidité
 * ✅ Si CMF reste positif sur plusieurs bougies → Les acheteurs dominent, entrée long possible.
 * ✅ Si CMF devient négatif après une hausse → Potentielle distribution avant chute.
 * 🚀 Prochaine étape ?
 *
 * ✅ Ajouter un seuil pour ne prendre des trades que si CMF > 0.2 ou < -0.2 ?
 * ✅ Coupler CMF avec l’Order Flow pour voir si les ordres passifs confirment la liquidité ?
 * ✅ Associer CMF avec le Market Profile pour voir si le volume est sur un niveau clé ?
 *
 *
 * Dis-moi comment tu veux l’améliorer ! 🚀🔥
 *
 *  A IMPLEMENTER : Analyse du carnet d'ordre, des liquidités visibles/invisibles.
 *  Détection d’absorption, spoofing, empilement d’ordres.
 */
@Service
public class LiquidityFilter {

    /**
     * Calcule le Chaikin Money Flow (CMF) sur une période donnée
     *
     * @param closes Liste des prix de clôture.
     * @param highs Liste des plus hauts.
     * @param lows Liste des plus bas.
     * @param volumes Liste des volumes échangés.
     * @return Valeur du CMF (-1 = pression vendeuse, 1 = pression acheteuse).
     */
    public double calculateCMF(List<Double> closes, List<Double> highs, List<Double> lows, List<Double> volumes) {
        if (closes.size() != highs.size() || closes.size() != lows.size() || closes.size() != volumes.size()) {
            throw new IllegalArgumentException("Les listes doivent avoir la même taille.");
        }

        double sumMFVolume = 0;
        double sumVolume = 0;

        for (int i = 0; i < closes.size(); i++) {
            double mfMultiplier = ((closes.get(i) - lows.get(i)) - (highs.get(i) - closes.get(i))) / (highs.get(i) - lows.get(i));
            double mfVolume = mfMultiplier * volumes.get(i);

            sumMFVolume += mfVolume;
            sumVolume += volumes.get(i);
        }

        return sumVolume == 0 ? 0 : sumMFVolume / sumVolume;
    }
}

