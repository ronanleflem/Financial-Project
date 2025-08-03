package finance.project.api.repositories;

import finance.project.api.entities.FilterSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilterSignalRepository extends JpaRepository<FilterSignal, Long> {
}