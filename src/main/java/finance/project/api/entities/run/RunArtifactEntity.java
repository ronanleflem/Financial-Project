package finance.project.api.entities.run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "run_artifact")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunArtifactEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "path", nullable = false, length = 2000)
    private String path;

    @Lob
    @Column(name = "meta")
    private String meta;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

