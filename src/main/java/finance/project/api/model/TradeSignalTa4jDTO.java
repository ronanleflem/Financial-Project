package finance.project.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeSignalTa4jDTO {

    private String symbol;             // Nom de l’actif (ex: 6EU0)
    private String timeframe;          // Timeframe utilisé (ex: "1min", "5min", etc.)
    private String direction;          // BUY / SELL
    private double price;              // Prix d’exécution
    private LocalDateTime timestamp;   // Date/heure du signal
}