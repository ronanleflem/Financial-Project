package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder(toBuilder = true)
@Data
public class CandleDTO {
    private Long id;
    private String timeframe;
    private SymbolDTO symbol; // Utilisation du DTO au lieu de l'entité
    private String symbolFuture;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime date;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;
    private BigDecimal volumeAverage;
    private BigDecimal openInterest;
}
