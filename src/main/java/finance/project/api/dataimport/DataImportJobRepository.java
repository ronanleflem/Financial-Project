package finance.project.api.dataimport;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DataImportJobRepository extends JpaRepository<DataImportJob, String> {
}
