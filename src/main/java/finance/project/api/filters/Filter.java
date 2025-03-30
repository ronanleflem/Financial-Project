package finance.project.api.filters;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeRequestDTO;

import java.util.List;

//Interface commune a tous les filtres
public interface Filter {
    /**
     * Évalue un filtre sur une liste de prix et retourne un score.
     * Score bas = conforme, Score élevé = anomalie ou filtre non respecté.
     *
     * @param priceChanges Liste des variations de prix.
     * @return Score du filtre (0 = conforme, > 0 = anomalie détectée).
     */
    double evaluate(List<Double> priceChanges);
}