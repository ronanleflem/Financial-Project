package finance.project.api.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.repositories.projections.CandleAvailabilityProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CandleAvailabilityProjectionTest {

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @Test
    void findAvailabilitySummaryAggregatesCandles() {
        Symbol symbol = symbolRepository.save(Symbol.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build());

        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0);

        candleRepository.save(Candle.builder()
                .symbol(symbol)
                .timeframe("daily")
                .date(start)
                .open(BigDecimal.ONE)
                .close(BigDecimal.ONE)
                .high(BigDecimal.ONE)
                .low(BigDecimal.ONE)
                .build());

        candleRepository.save(Candle.builder()
                .symbol(symbol)
                .timeframe("daily")
                .date(end)
                .open(BigDecimal.TEN)
                .close(BigDecimal.TEN)
                .high(BigDecimal.TEN)
                .low(BigDecimal.TEN)
                .build());

        List<CandleAvailabilityProjection> projections = candleRepository.findAvailabilitySummary();

        assertThat(projections).hasSize(1);

        CandleAvailabilityProjection projection = projections.get(0);
        assertThat(projection.getSymbol()).isEqualTo("AAPL");
        assertThat(projection.getBroker()).isEqualTo("DB");
        assertThat(projection.getTimeframe()).isEqualTo("daily");
        assertThat(projection.getStart()).isEqualTo(start);
        assertThat(projection.getEnd()).isEqualTo(end);
        assertThat(projection.getCount()).isEqualTo(2L);
        assertThat(projection.getUpdatedAt()).isEqualTo(end);
        assertThat(projection.getMarketType()).isEqualTo("NASDAQ");
    }
}
