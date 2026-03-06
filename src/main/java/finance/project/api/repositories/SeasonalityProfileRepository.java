package finance.project.api.repositories;

import finance.project.api.entities.quant.SeasonalityProfileEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SeasonalityProfileRepository extends JpaRepository<SeasonalityProfileEntity, Long> {
    List<SeasonalityProfileEntity> findBySpecIdAndDatasetIdOrderByCreatedAtAsc(String specId, String datasetId);
    List<SeasonalityProfileEntity> findBySpecIdOrderByCreatedAtAsc(String specId);
    List<SeasonalityProfileEntity> findByDatasetIdOrderByCreatedAtAsc(String datasetId);

    @Modifying
    @Transactional
    @Query(
            value = """
                    INSERT INTO seasonality_profiles (
                        symbol, timeframe, dim, bin, measure, score, n, baseline, lift,
                        metrics, `start`, `end`, spec_id, dataset_id, created_at
                    ) VALUES (
                        :symbol, :timeframe, :dim, :bin, :measure, :score, :n, :baseline, :lift,
                        :metrics, :start, :end, :specId, :datasetId, CURRENT_TIMESTAMP
                    )
                    ON DUPLICATE KEY UPDATE
                        score = VALUES(score),
                        n = VALUES(n),
                        baseline = VALUES(baseline),
                        lift = VALUES(lift),
                        metrics = VALUES(metrics)
                    """,
            nativeQuery = true
    )
    int upsert(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("dim") String dim,
            @Param("bin") Integer bin,
            @Param("measure") String measure,
            @Param("score") Double score,
            @Param("n") Integer n,
            @Param("baseline") Double baseline,
            @Param("lift") Double lift,
            @Param("metrics") String metrics,
            @Param("start") String start,
            @Param("end") String end,
            @Param("specId") String specId,
            @Param("datasetId") String datasetId
    );
}
