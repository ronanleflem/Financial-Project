package finance.project.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SymbolDTO {

    private UUID id;
    private String symbol;
    private String name;
    private String market;
}
