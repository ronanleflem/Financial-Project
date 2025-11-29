package finance.project.api.repositories;

import finance.project.api.entities.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, UUID> {

    @Transactional(readOnly = true)
    Optional<Symbol> findBySymbol(String symbol);

    @Transactional(readOnly = true)
    Optional<Symbol> findBySymbolAndCurrency(String symbol, String currency);
}