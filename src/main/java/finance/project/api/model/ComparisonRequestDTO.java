package finance.project.api.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class ComparisonRequestDTO {

    private String symbol1;
    private String symbol2;
    private LocalDate startDate;
    private LocalDate endDate;
}
