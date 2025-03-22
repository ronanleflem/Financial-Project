package finance.project.api.services;

import finance.project.api.entities.OrderFlow;
import finance.project.api.entities.PointOfInterest;
import finance.project.api.repositories.OrderFlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class OrderFlowService {

    private final OrderFlowRepository orderFlowRepository;

    /**
     * Récupère les volumes acheteurs sur des niveaux institutionnels pour une période donnée.
     */
    public List<Double> getBuyVolumes(String symbol, List<PointOfInterest> keyLevels, LocalDateTime startDate, LocalDateTime endDate) {
        List<Double> levels = keyLevels.stream()
                .map(PointOfInterest::getHigh) // ou getLow(), selon le design de ta POI
                .toList();

        return orderFlowRepository.findBuyVolumesForLevelsAndDate(symbol, levels, startDate, endDate);
    }

    /**
     * Récupère les volumes vendeurs sur des niveaux institutionnels pour une période donnée.
     */
    public List<Double> getSellVolumes(String symbol, List<PointOfInterest> keyLevels, LocalDateTime startDate, LocalDateTime endDate) {
        List<Double> levels = keyLevels.stream()
                .map(PointOfInterest::getLow) // ou getHigh(), selon ton usage
                .toList();

        return orderFlowRepository.findSellVolumesForLevelsAndDate(symbol, levels, startDate, endDate);
    }

    /**
     * Delta Volume global sur une période et un timeframe.
     */
    public double getDeltaVolume(String symbol, String timeframe, LocalDateTime startDate, LocalDateTime endDate) {
        List<OrderFlow> flows = orderFlowRepository.findBySymbolAndTimeframeAndDateBetween(symbol, timeframe, startDate, endDate);

        double totalBuy = flows.stream().mapToDouble(OrderFlow::getBuyVolume).sum();
        double totalSell = flows.stream().mapToDouble(OrderFlow::getSellVolume).sum();

        return totalBuy - totalSell;
    }
}
