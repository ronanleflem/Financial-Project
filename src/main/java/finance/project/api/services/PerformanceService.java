package finance.project.api.services;

import finance.project.api.entities.Performance;
import finance.project.api.repositories.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
}