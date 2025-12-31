package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TradeCompletedDTO(
        Long id,
        String strategyName,
        String runId,
        TradeSignalDTO.TradeType tradeType,
        String assetClass,
        double entryPrice,
        double stopLoss,
        double takeProfit,
        double exitPrice,
        double profitOrLoss,
        BigDecimal pnlPct,
        BigDecimal maxDrawdownPct,
        BigDecimal quantity,
        Integer cycleId,
        double confidenceScore,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime entryTimestamp,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime exitTimestamp,
        String symbol,
        List<IntermediateEntryDTO> intermediateEntries
) {
}
