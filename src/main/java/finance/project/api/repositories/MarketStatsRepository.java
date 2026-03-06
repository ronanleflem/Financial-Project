package finance.project.api.repositories;

import finance.project.api.entities.quant.MarketStatsEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MarketStatsRepository extends JpaRepository<MarketStatsEntity, Long> {
    List<MarketStatsEntity> findBySpecIdAndDatasetIdOrderByCreatedAtAsc(String specId, String datasetId);
    List<MarketStatsEntity> findBySpecIdOrderByCreatedAtAsc(String specId);
    List<MarketStatsEntity> findByDatasetIdOrderByCreatedAtAsc(String datasetId);

    @Modifying
    @Transactional
    @Query(
            value = """
                    INSERT INTO market_stats (
                        symbol, timeframe, `event`, condition_name, condition_value,
                        target, `split`, n, successes, p_hat, ci_low, ci_high, lift,
                        `start`, `end`, spec_id, dataset_id, created_at
                    ) VALUES (
                        :symbol, :timeframe, :event, :conditionName, :conditionValue,
                        :target, :split, :n, :successes, :pHat, :ciLow, :ciHigh, :lift,
                        :start, :end, :specId, :datasetId, CURRENT_TIMESTAMP
                    )
                    ON DUPLICATE KEY UPDATE
                        n = VALUES(n),
                        successes = VALUES(successes),
                        p_hat = VALUES(p_hat),
                        ci_low = VALUES(ci_low),
                        ci_high = VALUES(ci_high),
                        lift = VALUES(lift),
                        dataset_id = VALUES(dataset_id)
                    """,
            nativeQuery = true
    )
    int upsert(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("event") String event,
            @Param("conditionName") String conditionName,
            @Param("conditionValue") String conditionValue,
            @Param("target") String target,
            @Param("split") String split,
            @Param("n") Integer n,
            @Param("successes") Integer successes,
            @Param("pHat") Double pHat,
            @Param("ciLow") Double ciLow,
            @Param("ciHigh") Double ciHigh,
            @Param("lift") Double lift,
            @Param("start") String start,
            @Param("end") String end,
            @Param("specId") String specId,
            @Param("datasetId") String datasetId
    );
}
