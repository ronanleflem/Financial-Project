package finance.project.api.repositories;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CandleRepository extends JpaRepository<Candle, UUID> {

    List<Candle> findBySymbolAndDateBetween(Symbol symbol, LocalDate startDate, LocalDate endDate);
}
