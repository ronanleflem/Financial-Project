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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "trials",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_trials_run", columnNames = {"run_id", "trial_number"})
        },
        indexes = {
                @Index(name = "ix_trials_run_id", columnList = "run_id"),
                @Index(name = "ix_trials_trial_number", columnList = "trial_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "trial_number", nullable = false)
    private Integer trialNumber;

    @Lob
    @Column(name = "params_json", columnDefinition = "LONGTEXT")
    private String paramsJson;

    @Column(name = "objective_value")
    private Double objectiveValue;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "n_trades")
    private Integer nTrades;

    @Column(name = "max_dd")
    private Double maxDd;

    @Column(name = "sharpe")
    private Double sharpe;

    @Column(name = "sortino")
    private Double sortino;

    @Column(name = "cagr")
    private Double cagr;

    @Column(name = "hit_rate")
    private Double hitRate;

    @Column(name = "avg_r")
    private Double avgR;
}
