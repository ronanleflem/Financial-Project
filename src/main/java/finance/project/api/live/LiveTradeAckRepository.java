package finance.project.api.live;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveTradeAckRepository extends JpaRepository<LiveTradeAck, Long> {
  Optional<LiveTradeAck> findByUniqHash(String uniqHash);

  @Query("select a from LiveTradeAck a where a.tsOpenUtc >= :since order by a.tsOpenUtc desc")
  List<LiveTradeAck> findRecent(@Param("since") Instant since);
}
