package finance.project.api.universe.dto;

public record UniverseImportResponse(
        Long universeId,
        String code,
        String type,
        String provider
) {
}
