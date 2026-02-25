package finance.project.api.entities.quant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schema_migrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaMigrationEntity {
    @Id
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "applied_at", updatable = false, insertable = false)
    private Instant appliedAt;
}
