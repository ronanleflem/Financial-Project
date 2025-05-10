package finance.project.api.model;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TradeExitDTO {
    private double exitPrice;
    private LocalDateTime timestamp;

    @Override
    public String toString() {
        return "TradeExitDTO{" +
                "exitPrice=" + exitPrice +
                ", timestamp=" + timestamp +
                '}';
    }
}