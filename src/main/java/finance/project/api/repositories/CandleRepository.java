package finance.project.api.repositories;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<Candle, Long> {

    List<Candle> findBySymbolAndDateBetween(Symbol symbol, LocalDate startDate, LocalDate endDate);
    @Transactional(readOnly = true)
    List<Candle> findBySymbol(Symbol symbol);

    List<Candle> findBySymbolOrderByDateAsc(Symbol symbol);

    List<Candle> findBySymbolAndTimeframeOrderByDateAsc(Symbol symbol, String timeframe);
}
