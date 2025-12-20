package finance.project.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntermediateEntryDTO(
        int index,
        @JsonAlias("ts_utc") OffsetDateTime tsUtc,
        double qty,
        double price,
        @JsonAlias("grid_level") Double gridLevel,
        @JsonAlias("dd_pct") Double ddPct,
        @JsonAlias("palier_used") Double palierUsed
) {
}
