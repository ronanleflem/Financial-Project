package finance.project.api.model.run;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketStatsDataBlock(
        String symbol,
        @JsonAlias("symbols") List<String> symbols,
        String timeframe,
        Integer lookback,
        @JsonAlias("stats_pack") String statsPack,
        String session,
        @JsonAlias("include_weekends") Boolean includeWeekends
) implements DataBlock {
    public MarketStatsDataBlock {
        if ((symbols == null || symbols.isEmpty()) && symbol != null && !symbol.isBlank()) {
            symbols = List.of(symbol);
        }
        if ((symbol == null || symbol.isBlank()) && symbols != null && !symbols.isEmpty()) {
            symbol = symbols.stream().filter(value -> value != null && !value.isBlank()).findFirst().orElse(symbol);
        }
    }
}
