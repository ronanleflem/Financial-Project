package finance.project.api.entities;

import jakarta.persistence.*;
import lombok.*;

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
    private double value;

    private String symbol;

    private String comparedSymbol;
}