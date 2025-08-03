package finance.project.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "filter_signals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterSignal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filterName;
    private String symbol;
    private String timeframe;

    private LocalDateTime signalTime;
    private double baseClose;
    private int horizon;
    private double futureClose;
    private double variationPct;
    private boolean success;

    private int hourOfDay;
    private int dayOfWeek;
}