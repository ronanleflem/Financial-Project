package finance.project.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.TradeCompleted;
import finance.project.api.model.IntermediateEntryDTO;
import finance.project.api.model.TradeCompletedDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TradeCompletedMapper {

    private final ObjectMapper objectMapper;

    public TradeCompletedDTO toDto(TradeCompleted trade) {
        return new TradeCompletedDTO(
                trade.getId(),
                trade.getStrategyName(),
                trade.getRunId(),
                trade.getTradeType(),
                trade.getAssetClass(),
                trade.getEntryPrice(),
                trade.getStopLoss(),
                trade.getTakeProfit(),
                trade.getExitPrice(),
                trade.getProfitOrLoss(),
                trade.getPnlPct(),
                trade.getMaxDrawdownPct(),
                trade.getQuantity(),
                trade.getCycleId(),
                trade.getConfidenceScore(),
                trade.getEntryTimestamp(),
                trade.getExitTimestamp(),
                trade.getSymbol(),
                extractIntermediateEntries(trade.getMetaJson())
        );
    }

    private List<IntermediateEntryDTO> extractIntermediateEntries(String metaJson) {
        if (metaJson == null || metaJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(metaJson);
            JsonNode entriesNode = root.get("intermediate_entries");
            if (entriesNode == null || !entriesNode.isArray() || entriesNode.isEmpty()) {
                return Collections.emptyList();
            }
            return objectMapper.readerForListOf(IntermediateEntryDTO.class).readValue(entriesNode);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
