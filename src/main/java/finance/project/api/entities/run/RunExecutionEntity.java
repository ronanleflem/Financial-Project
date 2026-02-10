package finance.project.api.entities.run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "run_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunExecutionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "python_job_id")
    private String pythonJobId;

    @Column(name = "dispatch_attempts", nullable = false)
    private int dispatchAttempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RunLifecycleStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "last_dispatch_error", length = 2000)
    private String lastDispatchError;

    @Lob
    @Column(name = "result_json")
    private String resultJson;

    @Lob
    @Column(name = "warnings_json")
    private String warningsJson;
}

