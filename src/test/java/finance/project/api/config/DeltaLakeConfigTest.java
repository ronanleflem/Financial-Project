package finance.project.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DeltaLakeConfigTest {

    private final DeltaLakeConfig config = new DeltaLakeConfig("s3://datalake");

    @Test
    void resolvesPathWithCurrencySuffixForCryptoPairs() {
        String path = config.resolveTablePath("CRYPTO", "BINANCE", "SPOT", "ATOMUSDT");

        assertThat(path).isEqualTo("s3://datalake/CRYPTO/BINANCE/SPOT/USDT/ATOM/");
    }

    @Test
    void resolvesPathWithSeparatorBasedCurrency() {
        String pathUnderscore = config.resolveTablePath("ACTION", "IBKR", "NYSE", "AAPL_EUR");
        String pathColon = config.resolveTablePath("ACTION", "IBKR", "NYSE", "AAPL:USD");

        assertThat(pathUnderscore).isEqualTo("s3://datalake/ACTION/IBKR/SPOT/NYSE/EUR/AAPL/");
        assertThat(pathColon).isEqualTo("s3://datalake/ACTION/IBKR/SPOT/NYSE/USD/AAPL/");
    }

    @Test
    void sanitizesSegmentsAndFallsBackToUnknownCurrencyWhenMissing() {
        String path = config.resolveTablePath(" Action  ", "  ", "  ", "  T TE @#  ");

        assertThat(path).isEqualTo("s3://datalake/ACTION/UNKNOWN/SPOT/UNKNOWN/UNKNOWN/T_TE___/");
    }
}
