package finance.project.api.entities;

import finance.project.api.model.TradeSignalDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades_completed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeCompleted {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String strategyName;

    private String runId;

    @Enumerated(EnumType.STRING)
    private TradeSignalDTO.TradeType tradeType;

    private String assetClass;

    private String broker;

    private String exchange;

    private String currency;

    @Column(name = "market_type")
    private String marketType;

    private double entryPrice;
    private double stopLoss;
    private double takeProfit;
    private double exitPrice;

    private double profitOrLoss; // par exemple en points ou en valeur monétaire

    private BigDecimal pnlPct;

    private BigDecimal maxDrawdownPct;

    private BigDecimal quantity;

    private Integer cycleId;

    private double confidenceScore;

    private LocalDateTime entryTimestamp;
    private LocalDateTime exitTimestamp;
    private String symbol;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String metaJson;
}
