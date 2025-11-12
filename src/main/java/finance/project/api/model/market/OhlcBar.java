package finance.project.api.model.market;

import java.time.Instant;

public record OhlcBar(
        Instant time,
        double open,
        double high,
        double low,
        double close,
        double volume
) {
}
