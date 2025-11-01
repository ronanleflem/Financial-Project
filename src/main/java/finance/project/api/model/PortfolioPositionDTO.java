package finance.project.api.model;

/**
 * Détail d'une position de portefeuille renvoyée par un broker.
 */
public record PortfolioPositionDTO(
        String broker,
        String account,
        String symbol,
        String description,
        String securityType,
        String currency,
        String exchange,
        Integer contractId,
        double position,
        double marketPrice,
        double marketValue,
        double averageCost,
        double unrealizedPnL,
        double realizedPnL,
        double relativeValue
) {}
