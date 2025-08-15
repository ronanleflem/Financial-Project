package finance.project.api.factory;

import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;

@Component
public class SyntheticCandleFactory {

    public static final String SYNTHETIC_SECONDARY = "SYNTHETIC_SECONDARY";
    public static final String SYNTHETIC_EMPTY     = "SYNTHETIC_EMPTY";

    public CandleDTO buildSyntheticSecondary(LocalDateTime minute,
                                             CandleDTO prev, CandleDTO next,
                                             CandleDTO secondarySample,
                                             String timeframe,
                                             SymbolDTO baseSymbol) {

        BigDecimal open  = prev.getClose();
        BigDecimal close = next.getOpen();

        BigDecimal sOpen  = secondarySample.getOpen();
        BigDecimal sClose = secondarySample.getClose();
        BigDecimal sHigh  = secondarySample.getHigh();
        BigDecimal sLow   = secondarySample.getLow();

        BigDecimal secUpWick   = sHigh.subtract(sOpen.max(sClose));
        BigDecimal secLowWick  = sOpen.min(sClose).subtract(sLow).abs();
        BigDecimal secBody     = sClose.subtract(sOpen).abs();
        BigDecimal synBody     = close.subtract(open).abs();

        BigDecimal epsilon = new BigDecimal("0.000000001");
        BigDecimal factor  = synBody.divide(secBody.max(epsilon), MathContext.DECIMAL64);

        BigDecimal hiBase = open.max(close);
        BigDecimal loBase = open.min(close);

        BigDecimal high = hiBase.add(secUpWick.max(BigDecimal.ZERO).multiply(factor));
        BigDecimal low  = loBase.subtract(secLowWick.max(BigDecimal.ZERO).multiply(factor));

        return CandleDTO.builder()
                .date(minute)
                .open(open).close(close)
                .high(high.max(open.max(close)))
                .low(low.min(open.min(close)))
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe)
                .symbol(baseSymbol)
                .symbolFuture(SYNTHETIC_SECONDARY)
                .build();
    }

    public CandleDTO buildSyntheticEmpty(LocalDateTime minute,
                                         CandleDTO prev, CandleDTO next,
                                         String timeframe,
                                         SymbolDTO baseSymbol) {
        BigDecimal open  = prev.getClose();
        BigDecimal close = next.getOpen();
        BigDecimal high  = open.max(close);
        BigDecimal low   = open.min(close);

        return CandleDTO.builder()
                .date(minute)
                .open(open).close(close)
                .high(high).low(low)
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe)
                .symbol(baseSymbol)
                .symbolFuture(SYNTHETIC_EMPTY)
                .build();
    }
}
