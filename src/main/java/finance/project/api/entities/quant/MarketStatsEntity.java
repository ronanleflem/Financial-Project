package finance.project.api.entities.quant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "market_stats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_market_stats",
                        columnNames = {
                                "symbol", "timeframe", "event", "condition_name", "condition_value",
                                "target", "split", "start", "end", "spec_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "ix_market_stats_lookup",
                        columnList = "symbol,timeframe,event,condition_name,condition_value,target,split"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketStatsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 64)
    private String symbol;

    @Column(name = "timeframe", nullable = false, length = 64)
    private String timeframe;

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "condition_name")
    private String conditionName;

    @Column(name = "condition_value")
    private String conditionValue;

    @Column(name = "target", nullable = false)
    private String target;

    @Column(name = "split", nullable = false, length = 64)
    private String split;

    @Column(name = "n", nullable = false)
    private Integer n;

    @Column(name = "successes", nullable = false)
    private Integer successes;

    @Column(name = "p_hat", nullable = false)
    private Double pHat;

    @Column(name = "ci_low")
    private Double ciLow;

    @Column(name = "ci_high")
    private Double ciHigh;

    @Column(name = "lift", nullable = false)
    private Double lift;

    @Column(name = "start", nullable = false)
    private String start;

    @Column(name = "end", nullable = false)
    private String end;

    @Column(name = "spec_id")
    private String specId;

    @Column(name = "dataset_id")
    private String datasetId;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;
}
