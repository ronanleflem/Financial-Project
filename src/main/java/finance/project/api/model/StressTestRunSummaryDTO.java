package finance.project.api.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StressTestRunSummaryDTO {
    private String runId;
    private LocalDateTime createdAt;
    private String strategyId;
    private String symbol;
    private String assetClass;
    private String timeframe;
    private String status;
    private boolean hasSummary;
    private List<String> modes;
}
