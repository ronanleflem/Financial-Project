package finance.project.api.model;


import finance.project.api.entities.Symbol;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
public class CandleDTO {
    private Long id;
    private String timeframe;
    private SymbolDTO symbol; // Utilisation du DTO au lieu de l'entité
    private String symbolFuture;
    private LocalDateTime date;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;
    private BigDecimal volumeAverage;
    private BigDecimal openInterest;



}
