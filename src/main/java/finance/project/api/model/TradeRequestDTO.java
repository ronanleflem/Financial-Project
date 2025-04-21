package finance.project.api.model;

import lombok.*;

import java.time.LocalDateTime;
@Builder
@AllArgsConstructor
@Getter @Setter
public class TradeRequestDTO {
    private final TradeSignalDTO tradeSignal;
    private final LocalDateTime timestamp;
    private final double bidPrice;
    private final double askPrice;
    private final double volatility;
    private final double volume;
    private final String symbol;

    public TradeRequestDTO(TradeSignalDTO tradeSignal, double bidPrice, double askPrice, double volatility, double volume, String symbol) {
        this.tradeSignal = tradeSignal;
        this.symbol = symbol;
        this.timestamp = LocalDateTime.now(); // Horodatage automatique
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.volatility = volatility;
        this.volume = volume;
    }

    public TradeRequestDTO(TradeSignalDTO tradeSignal) {
        this(tradeSignal, 0.0, 0.0, 0.0, 0.0, "EURUSD"); // Valeurs par défaut si marché non fourni
    }

    // Getters
    public TradeSignalDTO getTradeSignal() { return tradeSignal; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getBidPrice() { return bidPrice; }
    public double getAskPrice() { return askPrice; }
    public double getVolatility() { return volatility; }
    public double getVolume() { return volume; }

    @Override
    public String toString() {
        return "TradeRequestDTO{" +
                "tradeSignal=" + tradeSignal +
                ", timestamp=" + timestamp +
                ", bidPrice=" + bidPrice +
                ", askPrice=" + askPrice +
                ", volatility=" + volatility +
                ", volume=" + volume +
                '}';
    }
}
