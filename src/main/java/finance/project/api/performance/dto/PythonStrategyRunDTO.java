package finance.project.api.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PythonStrategyRunDTO {
    private String strategyId;
    private String runId;
    private String assetClass;
    private String universe;
    private String timeframe;
    private String symbol;
    private String comparedSymbol;
    private OffsetDateTime startTsUtc;
    private OffsetDateTime endTsUtc;

    private BigDecimal winCount;
    private BigDecimal lossCount;
    private BigDecimal totalReturn;
    private BigDecimal maxDrawdown;
    private BigDecimal averageTrade;
    private BigDecimal averageSL;
    private BigDecimal averageTP;
    private BigDecimal rrMoyen;
    private BigDecimal totalNetReturn;
    private BigDecimal netWinCount;
    private BigDecimal netLossCount;
    private BigDecimal averageNetTrade;

    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private BigDecimal returnPct;
    private BigDecimal maxDrawdownPct;
    private BigDecimal volatilityPct;
    private BigDecimal sharpe;
    private BigDecimal sortino;
    private BigDecimal winratePct;

    private Map<String, Object> extra;
}
