package finance.project.api.utils;

import finance.project.api.model.TradeSignalDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
@AllArgsConstructor
public class StrategyResult {
    private List<TradeSignalDTO> signals;
    private Map<String, Double> performance;
}
