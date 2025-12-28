package finance.project.api.bootstrap;

import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BootstrapDataTest {

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    CandleRepository candleRepository;

    BootstrapData bootstrapData;

    @BeforeEach
    void setUp() {
        bootstrapData = new BootstrapData(candleRepository,symbolRepository, new RestTemplate());
    }

    @Test
    void testRun() throws Exception {
        bootstrapData.run();

        assertThat(symbolRepository.count()).isEqualTo(3);
        assertThat(candleRepository.count()).isEqualTo(3);
    }
}
