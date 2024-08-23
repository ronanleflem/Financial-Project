package finance.project.api.repositories;

import finance.project.api.entities.FinancialData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChartRepository extends JpaRepository<FinancialData, UUID> {

    List<FinancialData> findBySymbolAndDateBetween(String symbol, LocalDate startDate, LocalDate endDate);
}
