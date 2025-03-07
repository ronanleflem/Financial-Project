package finance.project.api.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PointOfInterest {
    private String name;
    private int type; // 0,1,2,3,4,5 (Daily High, FVG, etc.)
    private Double high;
    private Double low; // Optionnel si c'est un sommet/creux
    private boolean valid;
    private LocalDateTime datetime;
    private int count; // Nombre de fois où le marché revient dessus
    private double combled; // % du gap comblé
    private boolean signature;
    private int signatureType; // 0,1,2,3,4
    private String signatureNameType; // IOFED, classique
}