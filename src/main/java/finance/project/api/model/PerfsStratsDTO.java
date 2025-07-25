package finance.project.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
public class PerfsStratsDTO {
    private String name;
    private String runId;
    private BigDecimal winCount;
    private BigDecimal lossCount;
    private BigDecimal totalReturn; // en pips
    private BigDecimal maxDrawdown;
    private BigDecimal averageTrade;
    private BigDecimal averageSL; // en pips
    private BigDecimal averageTP; // en pips
    private String symbol;
    private String comparedSymbol;
    private LocalDateTime startStrategy;
    private LocalDateTime endStrategy;
    private BigDecimal rrMoyen;
    private BigDecimal totalNetReturn;
    private BigDecimal netWinCount;
    private BigDecimal netLossCount;
    private BigDecimal averageNetTrade;
    private String timeframe;

}
