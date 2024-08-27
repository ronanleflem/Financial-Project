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
        if (candleRepository.count() == 0) {
            Candle chart1 = createCandle("AAPL", "100.00", "110.00", "115.00", "95.00", "1000000");
            Candle chart2 = createCandle("EURUSD", "110.00", "120.00", "125.00", "105.00", "1100000");
            Candle chart3 = createCandle("GOLD", "110.00", "120.00", "125.00", "105.00", "1100000");

            candleRepository.saveAll(Arrays.asList(chart1, chart2, chart3));
        }
    }

    private Candle createCandle(String symbol, String open, String close, String high, String low, String volume) {
        return Candle.builder()
                .symbol(symbol)
                .date(LocalDate.now())
                .open(new BigDecimal(open))
                .close(new BigDecimal(close))
                .high(new BigDecimal(high))
                .low(new BigDecimal(low))
                .volume(new BigDecimal(volume))
                .build();
    }

    private void loadSymbolData() {
        if (symbolRepository.count() == 0) {
            Symbol symbol1 = createSymbol("AAPL", "Apple Inc.", "NASDAQ");
            Symbol symbol2 = createSymbol("EURUSD", "Euro to US Dollar", "Forex");
            Symbol symbol3 = createSymbol("GOLD", "Gold Futures", "Commodities");

            symbolRepository.saveAll(Arrays.asList(symbol1, symbol2, symbol3));
        }
    }

    private Symbol createSymbol(String symbol, String name, String market) {
        return Symbol.builder()
                .symbol(symbol)
                .name(name)
                .market(market)
                .build();
    }



}
