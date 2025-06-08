package finance.project.api.repositories;

import finance.project.api.entities.TradeCompleted;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeCompletedRepository extends JpaRepository<TradeCompleted, Long> {
    List<TradeCompleted> findByStrategyName(String strategyName);

    List<TradeCompleted> findByStrategyNameAndRunId(String strategyName, String runId);
}