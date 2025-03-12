package finance.project.api.repositories;

import finance.project.api.entities.Symbology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SymbologyRepository extends JpaRepository<Symbology, Long> {
    List<Symbology> findBySymbol(String symbol);
}