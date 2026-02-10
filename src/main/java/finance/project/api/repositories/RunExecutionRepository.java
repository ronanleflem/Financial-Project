package finance.project.api.repositories;

import finance.project.api.entities.run.RunExecutionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunExecutionRepository extends JpaRepository<RunExecutionEntity, Long> {
    Optional<RunExecutionEntity> findByRequestId(String requestId);
}

