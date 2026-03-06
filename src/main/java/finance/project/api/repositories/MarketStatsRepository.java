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
                        p_mean, p_map, hdi_low, hdi_high, lift_freq, lift_bayes,
                        p_value, q_value, significant, insufficient,
                        `start`, `end`, spec_id, dataset_id, created_at
                    ) VALUES (
                        :symbol, :timeframe, :event, :conditionName, :conditionValue,
                        :target, :split, :n, :successes, :pHat, :ciLow, :ciHigh, :lift,
                        :pMean, :pMap, :hdiLow, :hdiHigh, :liftFreq, :liftBayes,
                        :pValue, :qValue, :significant, :insufficient,
                        :start, :end, :specId, :datasetId, CURRENT_TIMESTAMP
                    )
                    ON DUPLICATE KEY UPDATE
                        n = VALUES(n),
                        successes = VALUES(successes),
                        p_hat = VALUES(p_hat),
                        ci_low = VALUES(ci_low),
                        ci_high = VALUES(ci_high),
                        lift = VALUES(lift),
                        p_mean = VALUES(p_mean),
                        p_map = VALUES(p_map),
                        hdi_low = VALUES(hdi_low),
                        hdi_high = VALUES(hdi_high),
                        lift_freq = VALUES(lift_freq),
                        lift_bayes = VALUES(lift_bayes),
                        p_value = VALUES(p_value),
                        q_value = VALUES(q_value),
                        significant = VALUES(significant),
                        insufficient = VALUES(insufficient),
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
            @Param("pMean") Double pMean,
            @Param("pMap") Double pMap,
            @Param("hdiLow") Double hdiLow,
            @Param("hdiHigh") Double hdiHigh,
            @Param("liftFreq") Double liftFreq,
            @Param("liftBayes") Double liftBayes,
            @Param("pValue") Double pValue,
            @Param("qValue") Double qValue,
            @Param("significant") Boolean significant,
            @Param("insufficient") Boolean insufficient,
            @Param("start") String start,
            @Param("end") String end,
            @Param("specId") String specId,
            @Param("datasetId") String datasetId
    );
}
