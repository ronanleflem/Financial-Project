package finance.project.api.bootstrap;

import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BootstrapDataTest {

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    CandleRepository candleRepository;

    BootstrapData bootstrapData;

    @BeforeEach
    void setUp() {
        bootstrapData = new BootstrapData(candleRepository,symbolRepository);
    }

    @Test
    void testRun() throws Exception {
        bootstrapData.run();

        assertThat(symbolRepository.count()).isEqualTo(3);
        assertThat(candleRepository.count()).isEqualTo(3);
    }
}