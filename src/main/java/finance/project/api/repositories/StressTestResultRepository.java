package finance.project.api.repositories;

import finance.project.api.entities.StressTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StressTestResultRepository extends JpaRepository<StressTestResult, Long> {
    List<StressTestResult> findByRunId(String runId);
}
