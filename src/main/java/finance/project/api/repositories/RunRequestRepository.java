package finance.project.api.repositories;

import finance.project.api.entities.run.RunRequestEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunRequestRepository extends JpaRepository<RunRequestEntity, Long> {
    Optional<RunRequestEntity> findByRequestId(String requestId);
    boolean existsByRequestId(String requestId);
}

