package finance.project.api.repositories;

import finance.project.api.entities.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    @Query("SELECT DISTINCT p.strategyName FROM Performance p")
    Set<String> getStrategyNames();
}
