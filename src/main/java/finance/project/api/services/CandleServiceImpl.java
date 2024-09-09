package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import finance.project.api.entities.Symbol;
import finance.project.api.model.SymbolDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CandleServiceImpl implements CandleService {

    private Map<UUID, CandleDTO> chartMap;

    public CandleServiceImpl() {
        this.chartMap = new HashMap<>();

        // Création des objets Symbol
        Symbol aaplSymbol = Symbol.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build();

        Symbol eurusdSymbol = Symbol.builder()
                .id(UUID.randomUUID())
                .symbol("EURUSD")
                .name("Euro to US Dollar")
                .market("Forex")
                .build();

        Symbol goldSymbol = Symbol.builder()
                .id(UUID.randomUUID())
                .symbol("GOLD")
                .name("Gold Futures")
                .market("Commodities")
                .build();

        CandleDTO chart1 = CandleDTO.builder()
                .symbol(aaplSymbol)
                .id(UUID.randomUUID())
                .date(LocalDate.now())
                .open(new BigDecimal("100.00"))
                .close(new BigDecimal("110.00"))
                .high(new BigDecimal("115.00"))
                .low(new BigDecimal("95.00"))
                .volume(new BigDecimal("1000000"))
                .build();

        CandleDTO chart2 = CandleDTO.builder()
                .symbol(eurusdSymbol)
                .id(UUID.randomUUID())
                .date(LocalDate.now())
                .open(new BigDecimal("110.00"))
                .close(new BigDecimal("120.00"))
                .high(new BigDecimal("125.00"))
                .low(new BigDecimal("105.00"))
                .volume(new BigDecimal("1100000"))
                .build();

        CandleDTO chart3 = CandleDTO.builder()
                .symbol(goldSymbol)
                .id(UUID.randomUUID())
                .date(LocalDate.now())
                .open(new BigDecimal("110.00"))
                .close(new BigDecimal("120.00"))
                .high(new BigDecimal("125.00"))
                .low(new BigDecimal("105.00"))
                .volume(new BigDecimal("1100000"))
                .build();

        chartMap.put(chart1.getId(), chart1);
        chartMap.put(chart2.getId(), chart2);
        chartMap.put(chart2.getId(), chart3);
    }

    @Override
    public List<CandleDTO> getCandles(SymbolDTO symbol, String interval) {
        return chartMap.values().stream()
                .filter(chartDataDTO -> chartDataDTO.getSymbol().equals(symbol.getSymbol()))
                .collect(Collectors.toList());
    }
}
