package finance.project.api.universe.dto;

public record UniverseCatalogDTO(
        String code,
        String name,
        String type,
        String provider,
        Integer approxSize,
        boolean importable
) {
}
