package finance.project.api.services;

import finance.project.api.entities.Performance;
import finance.project.api.model.PerfsStratsDTO;
import finance.project.api.repositories.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;

    public void savePerformance(String strategyName, Map<String, Double> performance) {
        List<Performance> performances = performance.entrySet().stream()
                .map(entry -> Performance.builder()
                        .strategyName(strategyName)
                        .metric(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
        performanceRepository.saveAll(performances);
    }

    public List<PerfsStratsDTO> getAllStrategyPerformances() {
        List<Performance> performances = performanceRepository.findAll();

        // Grouper les performances par stratégie
        Map<String, List<Performance>> grouped = performances.stream()
                .collect(Collectors.groupingBy(Performance::getStrategyName));

        List<PerfsStratsDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Performance>> entry : grouped.entrySet()) {
            String strategyName = entry.getKey();
            List<Performance> metrics = entry.getValue();

            // Si des metrics sont dupliquées, garder la dernière occurrence
            Map<String, Double> metricMap = new LinkedHashMap<>();
            for (Performance perf : metrics) {
                metricMap.put(perf.getMetric(), perf.getValue()); // overwrite = dernière valeur
            }

            PerfsStratsDTO dto = PerfsStratsDTO.builder()
                    .name(strategyName)
                    .winRate(toBigDecimal(metricMap.get("winRate")))
                    .lossRate(toBigDecimal(metricMap.get("lossRate")))
                    .totalReturn(toBigDecimal(metricMap.get("totalReturn")))
                    .maxDrawdown(toBigDecimal(metricMap.get("maxDrawdown")))
                    .averageTrade(toBigDecimal(metricMap.get("averageTrade")))
                    .averageSL(toBigDecimal(metricMap.get("averageSL")))
                    .averageTP(toBigDecimal(metricMap.get("averageTP")))
                    .build();

            result.add(dto);
        }

        return result;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
}