package finance.project.api.repositories;

import finance.project.api.entities.PointOfInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, Long> {

    List<PointOfInterest> findBySymbolAndTimeframe(String symbol, String timeframe);
}