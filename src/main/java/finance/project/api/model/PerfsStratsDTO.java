package finance.project.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
public class PerfsStratsDTO {
    private String name;
    private BigDecimal winRate;
    private BigDecimal lossRate;
    private BigDecimal totalReturn;
    private BigDecimal maxDrawdown;
    private BigDecimal averageTrade;
}
