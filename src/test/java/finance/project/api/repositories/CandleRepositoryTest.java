package finance.project.api.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.repositories.projections.CandleAvailabilityProjection;
import finance.project.api.support.AbstractMySqlIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CandleRepositoryTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @Test
    void findAvailabilitySummaryAggregatesCountsAndRanges() {
        Symbol apple = symbolRepository.save(Symbol.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build());

        Symbol microsoft = symbolRepository.save(Symbol.builder()
                .symbol("MSFT")
                .name("Microsoft")
                .market("NASDAQ")
                .build());

        candleRepository.saveAll(List.of(
                Candle.builder()
                        .symbol(apple)
                        .timeframe("1D")
                        .date(LocalDateTime.of(2024, 1, 1, 0, 0))
                        .open(BigDecimal.ONE)
                        .close(BigDecimal.ONE)
                        .high(BigDecimal.ONE)
                        .low(BigDecimal.ONE)
                        .build(),
                Candle.builder()
                        .symbol(apple)
                        .timeframe("1D")
                        .date(LocalDateTime.of(2024, 1, 3, 0, 0))
                        .open(BigDecimal.ONE)
                        .close(BigDecimal.ONE)
                        .high(BigDecimal.ONE)
                        .low(BigDecimal.ONE)
                        .build(),
                Candle.builder()
                        .symbol(microsoft)
                        .timeframe("1H")
                        .date(LocalDateTime.of(2024, 2, 1, 9, 0))
                        .open(BigDecimal.ONE)
                        .close(BigDecimal.ONE)
                        .high(BigDecimal.ONE)
                        .low(BigDecimal.ONE)
                        .build()
        ));

        List<CandleAvailabilityProjection> availability = candleRepository.findAvailabilitySummary();
        Map<String, CandleAvailabilityProjection> availabilityByKey = availability.stream()
                .collect(Collectors.toMap(
                        projection -> projection.getSymbol() + ":" + projection.getTimeframe(),
                        projection -> projection
                ));

        CandleAvailabilityProjection appleDaily = availabilityByKey.get("AAPL:1D");
        assertThat(appleDaily.getCount()).isEqualTo(2L);
        assertThat(appleDaily.getStart()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
        assertThat(appleDaily.getEnd()).isEqualTo(LocalDateTime.of(2024, 1, 3, 0, 0));
        assertThat(appleDaily.getMarketType()).isEqualTo("NASDAQ");

        CandleAvailabilityProjection microsoftHourly = availabilityByKey.get("MSFT:1H");
        assertThat(microsoftHourly.getCount()).isEqualTo(1L);
        assertThat(microsoftHourly.getStart()).isEqualTo(LocalDateTime.of(2024, 2, 1, 9, 0));
        assertThat(microsoftHourly.getEnd()).isEqualTo(LocalDateTime.of(2024, 2, 1, 9, 0));
        assertThat(microsoftHourly.getMarketType()).isEqualTo("NASDAQ");
    }
}
