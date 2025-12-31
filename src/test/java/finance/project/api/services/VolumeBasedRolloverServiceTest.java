package finance.project.api.services;

import static org.assertj.core.api.Assertions.assertThat;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.CandleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VolumeBasedRolloverServiceTest {

    private static final SymbolDTO BASE_SYMBOL = SymbolDTO.builder()
            .symbol("EURUSD")
            .name("Euro FX")
            .market("CME")
            .build();

    @Mock
    private CandleRepository candleRepository;

    @InjectMocks
    private VolumeBasedRolloverService service;

    @Test
    void rollsOverWhenSessionDominantChanges() {
        List<CandleDTO> candles = new ArrayList<>();

        // Analysis window for the first session: SYM1 dominates.
        candles.addAll(buildCandles("SYM1", LocalDateTime.of(2024, 6, 2, 22, 0), 3, 100, 10));
        candles.addAll(buildCandles("SYM2", LocalDateTime.of(2024, 6, 2, 22, 0), 3, 10, 5));

        // Session 1 candles: still output SYM1, but volumes for SYM2 rise to prepare the next dominance window.
        candles.addAll(buildCandles("SYM1", LocalDateTime.of(2024, 6, 3, 22, 0), 3, 50, 2));
        candles.addAll(buildCandles("SYM2", LocalDateTime.of(2024, 6, 3, 22, 0), 3, 200, 10));

        // Session 2 candles: SYM2 remains dominant.
        candles.addAll(buildCandles("SYM1", LocalDateTime.of(2024, 6, 4, 22, 0), 3, 40, 2));
        candles.addAll(buildCandles("SYM2", LocalDateTime.of(2024, 6, 4, 22, 0), 3, 300, 10));

        LocalDateTime start = LocalDateTime.of(2024, 6, 3, 22, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 4, 22, 3);

        List<CandleDTO> rolled = service.getDynamicRolloverCandlesSessionWithMinuteFallbackGlobalIndexed(
                candles,
                start,
                end,
                1
        );

        Optional<CandleDTO> sessionOneStart = rolled.stream()
                .filter(c -> LocalDateTime.of(2024, 6, 3, 22, 0).equals(c.getDate()))
                .findFirst();
        assertThat(sessionOneStart).isPresent();
        assertThat(sessionOneStart.get().getSymbolFuture()).isEqualTo("SYM1");

        Optional<CandleDTO> rollBridge = rolled.stream()
                .filter(c -> c.getSymbolFuture() != null
                        && c.getSymbolFuture().startsWith("SYNTHETIC_ROLL_BRIDGE_FWD"))
                .findFirst();
        assertThat(rollBridge).isPresent();
        assertThat(rollBridge.get().getDate()).isEqualTo(LocalDateTime.of(2024, 6, 4, 22, 0));

        Optional<CandleDTO> anchoredStart = rolled.stream()
                .filter(c -> "SYNTHETIC_ANCHORED:SYM2->SYM1".equals(c.getSymbolFuture()))
                .min(Comparator.comparing(CandleDTO::getDate));
        assertThat(anchoredStart).isPresent();
        assertThat(anchoredStart.get().getDate()).isEqualTo(LocalDateTime.of(2024, 6, 4, 22, 1));
    }

    @Test
    void noRolloverWhenOnlyOneContractIsAvailable() {
        List<CandleDTO> candles = new ArrayList<>();
        candles.addAll(buildCandles("SYM1", LocalDateTime.of(2024, 6, 2, 22, 0), 3, 100, 10));
        candles.addAll(buildCandles("SYM1", LocalDateTime.of(2024, 6, 3, 22, 0), 3, 120, 10));
        candles.addAll(buildCandles("SYM1", LocalDateTime.of(2024, 6, 4, 22, 0), 3, 140, 10));

        List<CandleDTO> rolled = service.getDynamicRolloverCandlesSessionWithMinuteFallbackGlobalIndexed(
                candles,
                LocalDateTime.of(2024, 6, 3, 22, 0),
                LocalDateTime.of(2024, 6, 4, 22, 3),
                1
        );

        assertThat(rolled).isNotEmpty();
        assertThat(rolled).allMatch(c -> "SYM1".equals(c.getSymbolFuture()));
    }

    private static List<CandleDTO> buildCandles(
            String symbolFuture,
            LocalDateTime start,
            int minutes,
            int startVolume,
            int stepVolume
    ) {
        List<CandleDTO> candles = new ArrayList<>();
        for (int i = 0; i < minutes; i++) {
            candles.add(candle(symbolFuture, start.plusMinutes(i), startVolume + (stepVolume * i)));
        }
        return candles;
    }

    private static CandleDTO candle(String symbolFuture, LocalDateTime date, int volume) {
        BigDecimal vol = BigDecimal.valueOf(volume);
        return CandleDTO.builder()
                .date(date)
                .open(BigDecimal.ONE)
                .close(BigDecimal.ONE)
                .high(BigDecimal.ONE)
                .low(BigDecimal.ONE)
                .volume(vol)
                .timeframe("1min")
                .symbol(BASE_SYMBOL)
                .symbolFuture(symbolFuture)
                .build();
    }
}
