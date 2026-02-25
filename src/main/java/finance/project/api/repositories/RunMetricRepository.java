package finance.project.api.repositories;

import finance.project.api.entities.quant.RunMetricEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunMetricRepository extends JpaRepository<RunMetricEntity, Long> {
    List<RunMetricEntity> findByRunId(String runId);
}
