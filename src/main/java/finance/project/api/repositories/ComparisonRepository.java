package finance.project.api.repositories;

import finance.project.api.entities.Comparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComparisonRepository extends JpaRepository<Comparison, UUID> {
    @Query("""
            SELECT c
            FROM Comparison c
            WHERE c.symbol1.id = :symbolId
              AND c.startDate >= :startDate
              AND c.endDate <= :endDate
            ORDER BY c.startDate ASC
            """)
    List<Comparison> findBySymbol1IdAndDateRange(
            @Param("symbolId") UUID symbolId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
