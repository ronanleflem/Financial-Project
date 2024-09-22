package finance.project.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class SymbolDTO {

    private UUID id;

    @NotNull
    @NotBlank
    @Size(max = 50)
    private String symbol;

    @NotNull
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @NotBlank
    @Size(max = 255)
    private String market;
}
