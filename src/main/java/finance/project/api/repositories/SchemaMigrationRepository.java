package finance.project.api.repositories;

import finance.project.api.entities.quant.SchemaMigrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaMigrationRepository extends JpaRepository<SchemaMigrationEntity, Integer> {
}
