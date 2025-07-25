package finance.project.api.services;

import finance.project.api.entities.Performance;
import finance.project.api.model.PerfsStratsDTO;
import finance.project.api.repositories.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;

    public void savePerformance(String strategyName, String runId, Map<String, Double> performance, String symbol, String comparedSymbol, String timeframe) {
        List<Performance> performances = performance.entrySet().stream()
            .map(entry -> Performance.builder()
                        .strategyName(strategyName)
                        .runId(runId)
                        .symbol(symbol)
                        .comparedSymbol(comparedSymbol)
                        .metric(entry.getKey())
                        .value(entry.getValue())
                        .timeframe(timeframe)
                        .build())
                .toList();
        performanceRepository.saveAll(performances);
    }

    public List<PerfsStratsDTO> getAllStrategyPerformances() {
        List<Performance> performances = performanceRepository.findAll();

        List<PerfsStratsDTO> result = new ArrayList<>();

        Map<String, List<Performance>> grouped = performances.stream()
                .collect(Collectors.groupingBy(Performance::getRunId));

        for (var entry : grouped.entrySet()) {
            String runId = entry.getKey();
            List<Performance> metrics = entry.getValue();
            if (metrics.isEmpty()) {
                continue;
            }
            Performance sample = metrics.get(0);
            Map<String, Double> metricMap = new LinkedHashMap<>();
            for (Performance perf : metrics) {
                metricMap.put(perf.getMetric(), perf.getValue());
            }
            PerfsStratsDTO dto = PerfsStratsDTO.builder()
                    .name(sample.getStrategyName())
                    .runId(runId)
                    .symbol(sample.getSymbol())
                    .comparedSymbol(sample.getComparedSymbol())
                    .winCount(toBigDecimal(metricMap.get("winCount")))
                    .lossCount(toBigDecimal(metricMap.get("lossCount")))
                    .totalReturn(toBigDecimal(metricMap.get("totalReturn")))
                    .maxDrawdown(toBigDecimal(metricMap.get("maxDrawdown")))
                    .averageTrade(toBigDecimal(metricMap.get("averageTrade")))
                    .averageSL(toBigDecimal(metricMap.get("averageSL")))
                    .averageTP(toBigDecimal(metricMap.get("averageTP")))
                    .rrMoyen(toBigDecimal(metricMap.get("RRmoyen")))
                    .startStrategy(toLocalDateTime(metricMap.get("startStrategy")))
                    .endStrategy(toLocalDateTime(metricMap.get("endStrategy")))
                    .totalNetReturn(toBigDecimal(metricMap.get("totalNetReturn")))
                    .netWinCount(toBigDecimal(metricMap.get("netWinCount")))
                    .netLossCount(toBigDecimal(metricMap.get("netLossCount")))
                    .averageNetTrade(toBigDecimal(metricMap.get("averageNetTrade")))
                    .timeframe(sample.getTimeframe())
                    .build();
            result.add(dto);
        }

        return result;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
    private LocalDateTime toLocalDateTime(Double value) {
        if (value == null) {
            return null;
        }
        long seconds = value.longValue();
        return LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);
    }
}