package finance.project.api.repositories;

import finance.project.api.entities.Killzone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderFlowRepository extends JpaRepository<Killzone, Long>{
}