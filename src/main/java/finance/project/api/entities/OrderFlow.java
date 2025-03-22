package finance.project.api.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_flow")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private String symbol;

    @NotNull
    private String timeframe; // Ex : "1min", "5min"

    @NotNull
    private LocalDateTime date; // Timestamp précis

    @NotNull
    private Double priceLevel; // Niveau institutionnel ou price cluster

    @NotNull
    private Double buyVolume; // Volume des ordres agressifs acheteurs

    @NotNull
    private Double sellVolume; // Volume des ordres agressifs vendeurs

    private Double deltaVolume; // buy - sell (optionnel si tu préfères le recalculer à la volée)

    private Double imbalance; // (buy - sell) / (buy + sell)

    private Boolean isPOI; // Optionnel : Si ce niveau est un POI marqué par tes filtres
}
