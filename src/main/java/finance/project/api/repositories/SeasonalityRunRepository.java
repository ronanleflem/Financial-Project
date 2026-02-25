package finance.project.api.repositories;

import finance.project.api.entities.quant.SeasonalityRunEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonalityRunRepository extends JpaRepository<SeasonalityRunEntity, Long> {
    Optional<SeasonalityRunEntity> findByRunId(String runId);
}
