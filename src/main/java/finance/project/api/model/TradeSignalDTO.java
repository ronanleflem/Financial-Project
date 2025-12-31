package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class TradeSignalDTO {
    public enum TradeType { LONG, SHORT }

    private final TradeType tradeType;
    private final double entryPrice;
    private final double stopLoss;
    private final double takeProfit;
    private final double confidenceScore;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final LocalDateTime timestamp;

    public boolean isSecondEntry() {
        return secondEntry;
    }

    private final boolean secondEntry;

    public String getSymbol() {
        return symbol;
    }

    private final String symbol;

    @JsonCreator
    public TradeSignalDTO(
            @JsonProperty("tradeType") TradeType tradeType,
            @JsonProperty("entryPrice") double entryPrice,
            @JsonProperty("stopLoss") double stopLoss,
            @JsonProperty("takeProfit") double takeProfit,
            @JsonProperty("confidenceScore") double confidenceScore,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("secondEntry") boolean secondEntry,
            @JsonProperty("symbol") String symbol
    ) {
        this.tradeType = tradeType;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.confidenceScore = confidenceScore;
        this.timestamp = timestamp;
        this.secondEntry = secondEntry;
        this.symbol = symbol;
    }

    public TradeSignalDTO(TradeType tradeType, double entryPrice, double stopLoss, double takeProfit, double confidenceScore, String symbol) {
        this.tradeType = tradeType;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.confidenceScore = confidenceScore;
        this.symbol = symbol;
        this.timestamp = LocalDateTime.now(); // Génère un horodatage automatique
        this.secondEntry = false;
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
                ", secondEntry=" + secondEntry +
                '}';
    }
}
