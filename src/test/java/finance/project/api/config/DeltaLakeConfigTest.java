package finance.project.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DeltaLakeConfigTest {

    private final DeltaLakeConfig config = new DeltaLakeConfig("s3://datalake");

    @Test
    void resolvesPathWithCurrencySuffixForCryptoPairs() {
        String path = config.resolveTablePath("CRYPTO", "ATOMUSDT");

        assertThat(path).isEqualTo("s3://datalake/CRYPTO/USDT/ATOM/");
    }

    @Test
    void resolvesPathWithSeparatorBasedCurrency() {
        String pathUnderscore = config.resolveTablePath("ACTION", "AAPL_EUR");
        String pathColon = config.resolveTablePath("ACTION", "AAPL:USD");

        assertThat(pathUnderscore).isEqualTo("s3://datalake/ACTION/EUR/AAPL/");
        assertThat(pathColon).isEqualTo("s3://datalake/ACTION/USD/AAPL/");
    }

    @Test
    void sanitizesSegmentsAndFallsBackToUnknownCurrencyWhenMissing() {
        String path = config.resolveTablePath(" Action  ", "  T TE @#  ");

        assertThat(path).isEqualTo("s3://datalake/ACTION/UNKNOWN/T_TE___/");
    }
}
