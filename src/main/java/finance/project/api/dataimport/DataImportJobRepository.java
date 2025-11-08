package finance.project.api.dataimport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataImportJobRepository extends JpaRepository<DataImportJob, String> {
    List<DataImportJob> findByStatus(DataImportJob.Status status);
}
