package finance.project.api.model;


import finance.project.api.entities.Symbol;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
public class CandleDTO {
    private UUID id;
    private SymbolDTO symbol; // Utilisation du DTO au lieu de l'entité
    private LocalDate date;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;
}
