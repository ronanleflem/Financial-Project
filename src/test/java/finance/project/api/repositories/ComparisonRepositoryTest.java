package finance.project.api.repositories;

import finance.project.api.entities.Comparison;
import finance.project.api.entities.Symbol;
import finance.project.api.support.AbstractMySqlIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ComparisonRepositoryTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private ComparisonRepository comparisonRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @Test
    void testSaveComparison() {
        Symbol symbol1 = symbolRepository.save(Symbol.builder().symbol("AAPL").name("Apple Inc.").market("NASDAQ").build());
        Symbol symbol2 = symbolRepository.save(Symbol.builder().symbol("GOOGL").name("Google LLC").market("NASDAQ").build());

        Comparison comparison = Comparison.builder()
                .symbol1(symbol1)
                .symbol2(symbol2)
                .performance1(BigDecimal.valueOf(10.5))
                .performance2(BigDecimal.valueOf(12.3))
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now())
                .build();

        comparisonRepository.save(comparison);

        Optional<Comparison> savedComparison = comparisonRepository.findById(comparison.getId());
        assertThat(savedComparison).isPresent();
        assertThat(savedComparison.get().getSymbol1().getSymbol()).isEqualTo("AAPL");
        assertThat(savedComparison.get().getPerformance1()).isEqualTo(BigDecimal.valueOf(10.5));
    }

    @Test
    void findBySymbol1IdAndDateRangeReturnsOrderedMatches() {
        Symbol symbol1 = symbolRepository.save(Symbol.builder().symbol("AAPL").name("Apple Inc.").market("NASDAQ").build());
        Symbol symbol2 = symbolRepository.save(Symbol.builder().symbol("MSFT").name("Microsoft").market("NASDAQ").build());

        Comparison first = Comparison.builder()
                .symbol1(symbol1)
                .symbol2(symbol2)
                .performance1(BigDecimal.valueOf(1.1))
                .performance2(BigDecimal.valueOf(2.2))
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 15))
                .build();

        Comparison second = Comparison.builder()
                .symbol1(symbol1)
                .symbol2(symbol2)
                .performance1(BigDecimal.valueOf(3.3))
                .performance2(BigDecimal.valueOf(4.4))
                .startDate(LocalDate.of(2024, 2, 1))
                .endDate(LocalDate.of(2024, 2, 15))
                .build();

        comparisonRepository.save(first);
        comparisonRepository.save(second);

        List<Comparison> results = comparisonRepository.findBySymbol1IdAndDateRange(
                symbol1.getId(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 3, 1)
        );

        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0).getStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(results.get(1).getStartDate()).isEqualTo(LocalDate.of(2024, 2, 1));
    }

    @Test
    void findBySymbol1IdAndDateRangeReturnsEmptyWhenNoData() {
        Symbol symbol1 = symbolRepository.save(Symbol.builder().symbol("AAPL").name("Apple Inc.").market("NASDAQ").build());
        Symbol symbol2 = symbolRepository.save(Symbol.builder().symbol("MSFT").name("Microsoft").market("NASDAQ").build());

        comparisonRepository.save(Comparison.builder()
                .symbol1(symbol1)
                .symbol2(symbol2)
                .performance1(BigDecimal.valueOf(1.1))
                .performance2(BigDecimal.valueOf(2.2))
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 15))
                .build());

        List<Comparison> results = comparisonRepository.findBySymbol1IdAndDateRange(
                symbol1.getId(),
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 3, 1)
        );

        assertThat(results).isEqualTo(List.of());
    }
}
