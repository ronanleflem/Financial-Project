package finance.project.api.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CandleFilterDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String session;
    private String marketCondition;
    private String newsEvent;
}
