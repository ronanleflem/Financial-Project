package finance.project.api.repositories;

import finance.project.api.entities.quant.TrialEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrialRepository extends JpaRepository<TrialEntity, Long> {
    List<TrialEntity> findByRunIdOrderByTrialNumberAsc(String runId);
}
