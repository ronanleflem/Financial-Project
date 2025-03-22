package finance.project.api.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Getter @Setter @Builder @Entity
@NoArgsConstructor @Table(name = "point_of_interest")
public class PointOfInterest {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private String symbol;

    @NotNull
    private int type; // 0,1,2,3,4,5 (Daily High, FVG, etc.)

    private Double high;
    private Double low; // Optionnel si c'est un sommet/creux

    @NotNull
    private String timeframe;

    @NotNull
    private boolean valid;

    @NotNull
    private LocalDateTime datetime;

    @NotNull
    private int count; // Nombre de fois où le marché revient dessus

    @NotNull
    private double combled; // % du gap comblé

    private boolean signature;
    private int signatureType; // 0,1,2,3,4
    private String signatureNameType; // IOFED, classique
}