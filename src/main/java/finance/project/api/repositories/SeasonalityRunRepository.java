package finance.project.api.repositories;

import finance.project.api.entities.quant.SeasonalityRunEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonalityRunRepository extends JpaRepository<SeasonalityRunEntity, Long> {
    Optional<SeasonalityRunEntity> findByRunId(String runId);
    List<SeasonalityRunEntity> findBySpecIdAndDatasetIdOrderByCreatedAtDesc(String specId, String datasetId);
    List<SeasonalityRunEntity> findBySpecIdOrderByCreatedAtDesc(String specId);
    List<SeasonalityRunEntity> findByDatasetIdOrderByCreatedAtDesc(String datasetId);
}
