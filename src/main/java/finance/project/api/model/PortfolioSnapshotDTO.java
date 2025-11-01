package finance.project.api.model;

import java.util.List;

/**
 * Vue agrégée d'un portefeuille renvoyée par un broker.
 */
public record PortfolioSnapshotDTO(
        String broker,
        double totalMarketValue,
        double availableLiquidity,
        List<PortfolioPositionDTO> positions,
        long asOf
) {}
