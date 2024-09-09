package finance.project.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
public class PerformanceComparisonDTO {
    private String symbol1;
    private BigDecimal performance1;
    private String symbol2;
    private BigDecimal performance2;
}