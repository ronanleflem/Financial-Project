package finance.project.api.utils;

import finance.project.api.model.CompletedTradeDTO;
import finance.project.api.model.TradeSignalDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyResult {
    private List<TradeSignalDTO> signals;
    private List<CompletedTradeDTO> completedTrades;
    private Map<String, Double> performance;
}
