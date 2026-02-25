package finance.project.api.entities.quant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "seasonality_runs",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_seasonality_runs_run_id", columnNames = {"run_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonalityRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "spec_id")
    private String specId;

    @Column(name = "dataset_id")
    private String datasetId;

    @Lob
    @Column(name = "out_dir", columnDefinition = "LONGTEXT")
    private String outDir;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Lob
    @Column(name = "best_summary", columnDefinition = "LONGTEXT")
    private String bestSummary;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;
}
