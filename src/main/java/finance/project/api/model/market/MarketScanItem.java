package finance.project.api.model.market;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarketScanItem(
        String symbol,
        String name,
        String exchange,
        Double last,
        Double changePct,
        Double marketCap,
        String currency,
        String sector,
        Double offHighPct,
        Double volumeRatio
) {
}
