package finance.project.api.universe;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UniverseRepository extends JpaRepository<Universe, Long> {

    Optional<Universe> findByCode(String code);

    @EntityGraph(attributePaths = "symbols")
    Optional<Universe> findWithSymbolsById(Long id);
}
