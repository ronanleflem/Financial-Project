package finance.project.api.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stress_test_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StressTestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_id")
    private String strategyId;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "asset_class")
    private String assetClass;

    private String symbol;

    private String timeframe;

    @Column(nullable = false)
    private String mode;

    @Column(name = "payload_json", columnDefinition = "JSON", nullable = false)
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
