package finance.project.api.repositories;

import finance.project.api.entities.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {}
