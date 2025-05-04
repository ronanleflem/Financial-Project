package finance.project.api.entities;

import finance.project.api.model.TradeSignalDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String strategyName;

    @Enumerated(EnumType.STRING)
    private TradeSignalDTO.TradeType tradeType;

    private double entryPrice;
    private double stopLoss;
    private double takeProfit;
    private double confidenceScore;

    private LocalDateTime timestamp;
}