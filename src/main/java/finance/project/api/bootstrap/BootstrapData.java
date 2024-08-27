package finance.project.api.bootstrap;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;

    @Override
    public void run(String... args) throws Exception {
        loadCandleData();
        loadSymbolData();
    }

    private void loadCandleData() {
        if(candleRepository.count() == 0) {

            Candle chart1 = Candle.builder()
                    .symbol("AAPL")
                    .date(LocalDate.now())
                    .open(new BigDecimal("100.00"))
                    .close(new BigDecimal("110.00"))
                    .high(new BigDecimal("115.00"))
                    .low(new BigDecimal("95.00"))
                    .volume(new BigDecimal("1000000"))
                    .build();

            Candle chart2 = Candle.builder()
                    .symbol("EURUSD")
                    .date(LocalDate.now())
                    .open(new BigDecimal("110.00"))
                    .close(new BigDecimal("120.00"))
                    .high(new BigDecimal("125.00"))
                    .low(new BigDecimal("105.00"))
                    .volume(new BigDecimal("1100000"))
                    .build();

            Candle chart3 = Candle.builder()
                    .symbol("GOLD")
                    .date(LocalDate.now())
                    .open(new BigDecimal("110.00"))
                    .close(new BigDecimal("120.00"))
                    .high(new BigDecimal("125.00"))
                    .low(new BigDecimal("105.00"))
                    .volume(new BigDecimal("1100000"))
                    .build();

            candleRepository.saveAll(Arrays.asList(chart1,chart2,chart3));
        }
    }

    private void loadSymbolData() {
        if(symbolRepository.count() == 0) {

            Symbol symbol1 = Symbol.builder()
                    .symbol("AAPL")
                    .name("Apple Inc.")
                    .market("NASDAQ")
                    .build();

            Symbol symbol2 = Symbol.builder()
                    .symbol("EURUSD")
                    .name("Euro to US Dollar")
                    .market("Forex")
                    .build();

            Symbol symbol3 = Symbol.builder()
                    .symbol("GOLD")
                    .name("Gold Futures")
                    .market("Commodities")
                    .build();

            symbolRepository.saveAll(Arrays.asList(symbol1, symbol2, symbol3));
        }
    }



}
