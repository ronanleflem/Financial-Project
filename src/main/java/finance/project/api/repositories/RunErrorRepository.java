package finance.project.api.repositories;

import finance.project.api.entities.run.RunErrorEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunErrorRepository extends JpaRepository<RunErrorEntity, Long> {
    List<RunErrorEntity> findByRequestIdOrderByCreatedAtAsc(String requestId);
}

