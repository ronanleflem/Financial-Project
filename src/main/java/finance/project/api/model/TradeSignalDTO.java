package finance.project.api.model;

import java.time.LocalDateTime;

public class TradeSignalDTO {
    public enum TradeType { LONG, SHORT }

    private final TradeType tradeType;
    private final double entryPrice;
    private final double stopLoss;
    private final double takeProfit;
    private final double confidenceScore;
    private final LocalDateTime timestamp;

    public TradeSignalDTO(TradeType tradeType, double entryPrice, double stopLoss, double takeProfit, double confidenceScore) {
        this.tradeType = tradeType;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.confidenceScore = confidenceScore;
        this.timestamp = LocalDateTime.now(); // Génère un horodatage automatique
    }

    // Getters
    public TradeType getTradeType() { return tradeType; }
    public double getEntryPrice() { return entryPrice; }
    public double getStopLoss() { return stopLoss; }
    public double getTakeProfit() { return takeProfit; }
    public double getConfidenceScore() { return confidenceScore; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "TradeSignalDTO{" +
                "tradeType=" + tradeType +
                ", entryPrice=" + entryPrice +
                ", stopLoss=" + stopLoss +
                ", takeProfit=" + takeProfit +
                ", confidenceScore=" + confidenceScore +
                ", timestamp=" + timestamp +
                '}';
    }
}