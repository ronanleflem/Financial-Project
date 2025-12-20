package finance.project.api.model;

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
        LocalDateTime entryTimestamp,
        LocalDateTime exitTimestamp,
        String symbol,
        List<IntermediateEntryDTO> intermediateEntries
) {
}
