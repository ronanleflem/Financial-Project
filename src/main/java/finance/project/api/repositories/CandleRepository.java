package finance.project.api.repositories;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandleRepository extends JpaRepository<Candle, Long> {

    @Query("SELECT c FROM Candle c WHERE c.symbolFuture = :symbolFuture AND c.date BETWEEN :startDate AND :endDate ORDER BY c.date ASC")
    List<Candle> findBySymbolFutureAndDateBetween(
            @Param("symbolFuture") String symbolFuture,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT c FROM Candle c WHERE c.symbolFuture = :symbolFuture AND c.date = :date")
    Optional<Candle> findBySymbolFutureAndDate(
            @Param("symbolFuture") String symbolFuture,
            @Param("date") LocalDateTime date
    );

    List<Candle> findBySymbolAndDateBetween(Symbol symbol, LocalDate startDate, LocalDate endDate);

    @Transactional(readOnly = true)
    List<Candle> findBySymbol(Symbol symbol);

    List<Candle> findBySymbolOrderByDateAsc(Symbol symbol);

    List<Candle> findBySymbolAndTimeframeOrderByDateAsc(Symbol symbol, String timeframe);

    List<Candle> findBySymbolAndTimeframeOrderByDateDesc(Symbol symbol, String timeframe);

    @Query("SELECT c FROM Candle c WHERE c.date BETWEEN :startDate AND :endDate ORDER BY c.date ASC")
    List<Candle> findByDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = "SELECT c FROM Candle c WHERE c.symbol = :symbol AND c.timeframe = :timeframe ORDER BY c.date ASC LIMIT :numberLastestCandles")
    List<Candle> findBySymbolAndTimeframeOrderByDateAscLimitNumberLatestCandle(
            @Param("symbol") Optional<Symbol> symbol,
            @Param("timeframe") String timeframe,
            @Param("numberLastestCandles") int numberLastestCandles);
    @Query(value = "SELECT c FROM Candle c WHERE c.symbol = :symbol AND c.timeframe = :timeframe ORDER BY c.date DESC LIMIT :numberLastestCandles")
    List<Candle> findBySymbolAndTimeframeOrderByDateDescLimitNumberLatestCandle(
            @Param("symbol") Optional<Symbol> symbol,
            @Param("timeframe") String timeframe,
            @Param("numberLastestCandles") int numberLastestCandles);
}
