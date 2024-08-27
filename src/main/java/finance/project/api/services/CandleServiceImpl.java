package finance.project.api.services;

import finance.project.api.model.CandleDTO;
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

        CandleDTO chart1 = CandleDTO.builder()
                .symbol("AAPL")
                .id(UUID.randomUUID())
                .date(LocalDate.now())
                .open(new BigDecimal("100.00"))
                .close(new BigDecimal("110.00"))
                .high(new BigDecimal("115.00"))
                .low(new BigDecimal("95.00"))
                .volume(new BigDecimal("1000000"))
                .build();

        CandleDTO chart2 = CandleDTO.builder()
                .symbol("EURUSD")
                .id(UUID.randomUUID())
                .date(LocalDate.now())
                .open(new BigDecimal("110.00"))
                .close(new BigDecimal("120.00"))
                .high(new BigDecimal("125.00"))
                .low(new BigDecimal("105.00"))
                .volume(new BigDecimal("1100000"))
                .build();

        CandleDTO chart3 = CandleDTO.builder()
                .symbol("GOLD")
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
    public List<CandleDTO> getCandles(String symbol, String interval) {
        // Symbol filter only -- Must add interval
        return chartMap.values().stream()
                .filter(chartDataDTO -> chartDataDTO.getSymbol().equalsIgnoreCase(symbol))
                .collect(Collectors.toList());
    }
}
