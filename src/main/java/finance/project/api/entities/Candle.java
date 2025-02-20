package finance.project.api.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name="candle")
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // SQLite ne supporte pas UUID, donc on utilise AUTO
    @Column(nullable = false, length = 255)
    private Long id; // Changement de UUID vers Long

    @NotNull
    @Column(nullable = false)
    private String timeframe;

    @ManyToOne
    @JoinColumn(name = "symbol_id", nullable = false, columnDefinition = "BINARY(16)")
    private Symbol symbol;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime date;

    @NotNull
    @Column(nullable = false, precision = 38, scale = 6)
    private BigDecimal open;

    @NotNull
    @Column(nullable = false, precision = 38, scale = 6)
    private BigDecimal close;

    @NotNull
    @Column(nullable = false, precision = 38, scale = 6)
    private BigDecimal high;

    @NotNull
    @Column(nullable = false, precision = 38, scale = 6)
    private BigDecimal low;

    @Column(nullable = true, precision = 38, scale = 6)
    private BigDecimal volume;

}