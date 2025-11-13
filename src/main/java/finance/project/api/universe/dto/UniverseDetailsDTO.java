package finance.project.api.universe.dto;

import java.util.List;

public record UniverseDetailsDTO(
        Long id,
        String code,
        String name,
        String type,
        String provider,
        List<String> symbols
) {
}
