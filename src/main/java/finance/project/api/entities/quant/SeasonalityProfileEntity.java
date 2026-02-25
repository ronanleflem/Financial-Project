package finance.project.api.entities.quant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
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
        name = "seasonality_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_seasonality_profiles",
                        columnNames = {
                                "symbol", "timeframe", "dim", "bin", "measure",
                                "start", "end", "spec_id", "dataset_id"
                        }
                )
        },
        indexes = {
                @Index(name = "ix_seasonality_profiles_lookup", columnList = "symbol,timeframe,dim,measure")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonalityProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 64)
    private String symbol;

    @Column(name = "timeframe", length = 64)
    private String timeframe;

    @Column(name = "dim", nullable = false)
    private String dim;

    @Column(name = "bin", nullable = false)
    private Integer bin;

    @Column(name = "measure", nullable = false)
    private String measure;

    @Column(name = "score")
    private Double score;

    @Column(name = "n")
    private Integer n;

    @Column(name = "baseline")
    private Double baseline;

    @Column(name = "lift")
    private Double lift;

    @Lob
    @Column(name = "metrics", columnDefinition = "LONGTEXT")
    private String metrics;

    @Column(name = "start")
    private String start;

    @Column(name = "end")
    private String end;

    @Column(name = "spec_id")
    private String specId;

    @Column(name = "dataset_id")
    private String datasetId;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;
}
