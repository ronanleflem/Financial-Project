package finance.project.api.services;

import finance.project.api.mappers.ChartDataMapper;
import finance.project.api.model.ChartDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChartServiceImpl implements ChartService {

    private Map<UUID,ChartDataDTO> chartMap;

    public ChartServiceImpl() {
        this.chartMap = new HashMap<>();

        ChartDataDTO chart1 = ChartDataDTO.builder()
                .symbol("AAPL")
                .id(UUID.randomUUID())
                .date(LocalDate.now())
                .open(new BigDecimal("100.00"))
                .close(new BigDecimal("110.00"))
                .high(new BigDecimal("115.00"))
                .low(new BigDecimal("95.00"))
                .volume(new BigDecimal("1000000"))
                .build();

        ChartDataDTO chart2 = ChartDataDTO.builder()
                .symbol("EURUSD")
                .id(UUID.randomUUID())
                .date(LocalDate.now())
                .open(new BigDecimal("110.00"))
                .close(new BigDecimal("120.00"))
                .high(new BigDecimal("125.00"))
                .low(new BigDecimal("105.00"))
                .volume(new BigDecimal("1100000"))
                .build();

        ChartDataDTO chart3 = ChartDataDTO.builder()
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
    public List<ChartDataDTO> getChartData(String symbol, String interval) {
        // Symbol filter only -- Must add interval
        return chartMap.values().stream()
                .filter(chartDataDTO -> chartDataDTO.getSymbol().equalsIgnoreCase(symbol))
                .collect(Collectors.toList());
    }
}
