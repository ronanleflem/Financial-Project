package finance.project.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Performance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String strategyName;

    private String metric; // Ex: "winRate", "profitFactor"

    @Column(name = "metric_value")
    private Double metricValue;

    /**
     * Identifier to group all metrics of the same strategy run. This allows
     * distinguishing multiple executions of the same strategy.
     */
    private String runId;

    private String symbol;

    private String comparedSymbol;

    private String timeframe;

    private String assetClass;

    private String universe;

    private LocalDateTime startStrategy;

    private LocalDateTime endStrategy;

    private BigDecimal winCount;

    private BigDecimal lossCount;

    private BigDecimal totalReturn;

    private BigDecimal maxDrawdown;

    private BigDecimal averageTrade;

    private BigDecimal averageSL;

    private BigDecimal averageTP;

    private BigDecimal rrMoyen;

    private BigDecimal totalNetReturn;

    private BigDecimal netWinCount;

    private BigDecimal netLossCount;

    private BigDecimal averageNetTrade;

    private BigDecimal initialCapital;

    private BigDecimal finalCapital;

    private BigDecimal returnPct;

    private BigDecimal maxDrawdownPct;

    private BigDecimal volatilityPct;

    private BigDecimal sharpe;

    private BigDecimal sortino;

    private BigDecimal winratePct;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String extraJson;
}
