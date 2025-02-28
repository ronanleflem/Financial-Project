package finance.project.api.repositories;

import finance.project.api.entities.Killzone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KillzoneRepository extends JpaRepository<Killzone, Long> {
    List<Killzone> findByYear(int year);
}