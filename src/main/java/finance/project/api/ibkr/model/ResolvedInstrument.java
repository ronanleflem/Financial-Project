package finance.project.api.ibkr.model;

/**
 * Metadata resolved from IBKR contract details.
 */
public record ResolvedInstrument(
        int conid,
        String symbol,
        String localSymbol,
        String tradingClass,
        String secType,
        String currency,
        String ibPrimaryExch,
        String normalizedExchange,
        String marketTypeNormalized
) {
}
