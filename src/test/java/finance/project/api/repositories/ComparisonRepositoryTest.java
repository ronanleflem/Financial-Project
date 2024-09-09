package finance.project.api.repositories;

import finance.project.api.entities.Comparison;
import finance.project.api.entities.Symbol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class ComparisonRepositoryTest {

    @Autowired
    private ComparisonRepository comparisonRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @Test
    public void testSaveComparison() {
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
}
