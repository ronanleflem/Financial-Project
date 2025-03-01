package finance.project.api.services;

import finance.project.api.repositories.OrderFlowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderFlowService {

    @Autowired
    private OrderFlowRepository orderFlowRepository;

    /**
     * Récupère les volumes acheteurs par niveau institutionnel. FIXME
     */
    public List<Double> getBuyVolumes(String symbol, List<Double> keyLevels) {
        return null; //orderFlowRepository.findBuyVolumesForLevels(symbol, keyLevels);
    }

    /**
     * Récupère les volumes vendeurs par niveau institutionnel. FIXME
     */
    public List<Double> getSellVolumes(String symbol, List<Double> keyLevels) {
        return null; //orderFlowRepository.findSellVolumesForLevels(symbol, keyLevels);
    }

    /**
     * Récupère le Delta Volume (différence entre les achats et les ventes).
     *
     * @param symbol Actif à analyser
     * @param timeframe Unité de temps (ex: M5, M15)
     * @return Delta Volume (positif = pression acheteuse, négatif = pression vendeuse) FIXME
     */
    public double getDeltaVolume(String symbol, String timeframe) {
        List<Double> buyVolumes = null; //orderFlowRepository.findBuyVolumes(symbol, timeframe);
        List<Double> sellVolumes = null; //orderFlowRepository.findSellVolumes(symbol, timeframe);

        if (buyVolumes.size() != sellVolumes.size() || buyVolumes.isEmpty()) {
            throw new IllegalArgumentException("Données Order Flow invalides.");
        }

        double totalBuyVolume = buyVolumes.stream().mapToDouble(Double::doubleValue).sum();
        double totalSellVolume = sellVolumes.stream().mapToDouble(Double::doubleValue).sum();

        return totalBuyVolume - totalSellVolume; // Delta Volume
    }
}