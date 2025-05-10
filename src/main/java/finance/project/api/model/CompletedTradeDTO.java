package finance.project.api.model;

import lombok.*;

import java.time.Duration;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompletedTradeDTO {

    private TradeSignalDTO entrySignal;
    private TradeExitDTO exitSignal;

    public double getProfit() {
        if (entrySignal == null || exitSignal == null) return 0.0;

        double entry = entrySignal.getEntryPrice();
        double exit = exitSignal.getExitPrice();

        return entrySignal.getTradeType() == TradeSignalDTO.TradeType.LONG
                ? exit - entry
                : entry - exit;
    }

    public Duration getHoldingTime() {
        return Duration.between(entrySignal.getTimestamp(), exitSignal.getTimestamp());
    }

    @Override
    public String toString() {
        return "CompletedTradeDTO{" +
                "entry=" + entrySignal +
                ", exit=" + exitSignal +
                ", profit=" + getProfit() +
                ", duration=" + getHoldingTime() +
                '}';
    }
}