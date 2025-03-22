package finance.project.api.repositories;

import finance.project.api.entities.Killzone;
import finance.project.api.entities.OrderFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderFlowRepository extends JpaRepository<OrderFlow, Long> {

    // Pour récupérer les volumes acheteurs sur des zones clés avec une période
    @Query("SELECT of.buyVolume FROM OrderFlow of " +
            "WHERE of.symbol = :symbol " +
            "AND of.priceLevel IN :levels " +
            "AND of.date BETWEEN :startDate AND :endDate")
    List<Double> findBuyVolumesForLevelsAndDate(
            @Param("symbol") String symbol,
            @Param("levels") List<Double> levels,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Idem pour sell volumes
    @Query("SELECT of.sellVolume FROM OrderFlow of " +
            "WHERE of.symbol = :symbol " +
            "AND of.priceLevel IN :levels " +
            "AND of.date BETWEEN :startDate AND :endDate")
    List<Double> findSellVolumesForLevelsAndDate(
            @Param("symbol") String symbol,
            @Param("levels") List<Double> levels,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Récupérer tous les OrderFlows sur une période et un timeframe
    List<OrderFlow> findBySymbolAndTimeframeAndDateBetween(String symbol, String timeframe, LocalDateTime start, LocalDateTime end);
}
