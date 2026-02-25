package finance.project.api.repositories;

import finance.project.api.entities.quant.ApiJobEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiJobRepository extends JpaRepository<ApiJobEntity, Long> {
    Optional<ApiJobEntity> findByJobId(String jobId);
}
