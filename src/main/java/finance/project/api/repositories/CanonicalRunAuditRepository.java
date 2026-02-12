package finance.project.api.repositories;

import finance.project.api.entities.run.CanonicalRunAuditEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanonicalRunAuditRepository extends JpaRepository<CanonicalRunAuditEntity, Long> {
    Optional<CanonicalRunAuditEntity> findByRequestId(String requestId);
}
