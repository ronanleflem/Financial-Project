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
public class PythonCompletedTradeDTO {
    private String strategyId;
    private String runId;
    private String symbol;
    private String assetClass;
    private String side;
    private Integer cycleId;
    private OffsetDateTime entryTimeUtc;
    private OffsetDateTime exitTimeUtc;
    private BigDecimal entryPrice;
    private BigDecimal exitPrice;
    private BigDecimal quantity;
    private BigDecimal grossPnl;
    private BigDecimal grossPnlPct;
    private BigDecimal maxDdPct;
    private Map<String, Object> meta;
}
