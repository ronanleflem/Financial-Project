package finance.project.api.universe;

import static org.assertj.core.api.Assertions.assertThat;

import finance.project.api.support.AbstractMySqlIntegrationTest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UniverseRepositoryIT extends AbstractMySqlIntegrationTest {

    @Autowired
    private UniverseRepository universeRepository;

    @Test
    void findByCodeReturnsMatchingUniverse() {
        Universe universe = Universe.builder()
                .code("core-equities")
                .name("Core Equities")
                .type(UniverseType.EQUITY)
                .provider("internal")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        universeRepository.save(universe);

        Optional<Universe> result = universeRepository.findByCode("core-equities");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Core Equities");
        assertThat(result.get().getProvider()).isEqualTo("internal");
    }
}
