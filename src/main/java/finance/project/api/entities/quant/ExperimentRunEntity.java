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
        name = "experiment_runs",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_experiment_runs_run_id", columnNames = {"run_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperimentRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "spec_id")
    private String specId;

    @Column(name = "dataset_id")
    private String datasetId;

    @Column(name = "status", length = 64)
    private String status;

    @Lob
    @Column(name = "objective", columnDefinition = "LONGTEXT")
    private String objective;

    @Lob
    @Column(name = "out_dir", columnDefinition = "LONGTEXT")
    private String outDir;

    @Column(name = "started_at", updatable = false, insertable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
