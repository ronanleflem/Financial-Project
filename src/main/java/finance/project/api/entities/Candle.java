package finance.project.api.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "symbol_id", nullable = false)
    private Symbol symbol;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @NotNull
    @Column(nullable = false)
    private BigDecimal open;

    @NotNull
    @Column(nullable = false)
    private BigDecimal close;

    @NotNull
    @Column(nullable = false)
    private BigDecimal high;

    @NotNull
    @Column(nullable = false)
    private BigDecimal low;

    @NotNull
    @Column(nullable = false)
    private BigDecimal volume;

}