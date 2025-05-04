package finance.project.api.repositories;

import finance.project.api.entities.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {}
