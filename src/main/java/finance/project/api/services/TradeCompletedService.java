package finance.project.api.services;

import finance.project.api.entities.TradeCompleted;
import finance.project.api.model.CompletedTradeDTO;
import finance.project.api.repositories.TradeCompletedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeCompletedService {

    private final TradeCompletedRepository tradeCompletedRepository;

    public void saveCompletedTrades(String strategyName, List<CompletedTradeDTO> completedTrades) {
        List<TradeCompleted> trades = completedTrades.stream()
                .map(dto -> TradeCompleted.builder()
                        .strategyName(strategyName)
                        .tradeType(dto.getEntrySignal().getTradeType())
                        .entryPrice(dto.getEntrySignal().getEntryPrice())
                        .stopLoss(dto.getEntrySignal().getStopLoss())
                        .takeProfit(dto.getEntrySignal().getStopLoss())
                        .exitPrice(dto.getExitSignal().getExitPrice())
                        .profitOrLoss(dto.getProfit())
                        .confidenceScore(dto.getEntrySignal().getConfidenceScore())
                        .entryTimestamp(dto.getEntrySignal().getTimestamp())
                        .exitTimestamp(dto.getExitSignal().getTimestamp())
                        .build())
                .toList();

        tradeCompletedRepository.saveAll(trades);
    }

    public List<TradeCompleted> getTradesByStrategy(String strategyName) {
        return tradeCompletedRepository.findByStrategyName(strategyName);
    }
}