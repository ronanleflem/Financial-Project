package finance.project.api.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PythonRunPayloadDTO {
    private PythonStrategyRunDTO run;
    private List<PythonCompletedTradeDTO> trades;
}
