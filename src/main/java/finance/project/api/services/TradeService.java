package finance.project.api.services;

import finance.project.api.entities.Trade;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.repositories.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;

    public void saveTrades(String strategyName, List<TradeSignalDTO> signals) {
        List<Trade> trades = signals.stream()
                .map(signal -> Trade.builder()
                        .strategyName(strategyName)
                        .tradeType(signal.getTradeType())
                        .entryPrice(signal.getEntryPrice())
                        .stopLoss(signal.getStopLoss())
                        .takeProfit(signal.getTakeProfit())
                        .confidenceScore(signal.getConfidenceScore())
                        .timestamp(signal.getTimestamp())
                        .build())
                .toList();
        tradeRepository.saveAll(trades);
    }
}