package finance.project.api.repositories;

import finance.project.api.entities.quant.ExperimentRunEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRunEntity, Long> {
    Optional<ExperimentRunEntity> findByRunId(String runId);
}
