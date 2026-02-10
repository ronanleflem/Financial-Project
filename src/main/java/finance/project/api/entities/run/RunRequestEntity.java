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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "run_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false, unique = true)
    private String requestId;

    @Column(name = "spec_type", nullable = false)
    private String specType;

    @Column(name = "catalog_version", nullable = false)
    private String catalogVersion;

    @Lob
    @Column(name = "payload_in", nullable = false)
    private String payloadIn;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Lob
    @Column(name = "spec_generated", nullable = false)
    private String specGenerated;

    @Column(name = "spec_hash", nullable = false, length = 64)
    private String specHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RunLifecycleStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

