package finance.project.api.repositories;

import finance.project.api.entities.run.RunArtifactEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunArtifactRepository extends JpaRepository<RunArtifactEntity, Long> {
    List<RunArtifactEntity> findByRequestIdOrderByCreatedAtAsc(String requestId);
}

