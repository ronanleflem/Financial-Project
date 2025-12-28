package finance.project.api.services;

import finance.project.api.entities.Performance;
import finance.project.api.model.PerfsStratsDTO;
import finance.project.api.repositories.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;

    public void savePerformance(String strategyName, String runId, Map<String, Double> performance, String symbol, String comparedSymbol, String timeframe) {
        Performance perf = Performance.builder()
                .strategyName(strategyName)
                .runId(runId)
                .symbol(symbol)
                .comparedSymbol(comparedSymbol)
                .timeframe(timeframe)
                .metric(null)
                .metricValue(0.0)
                .winCount(toBigDecimal(performance.get("winCount")))
                .lossCount(toBigDecimal(performance.get("lossCount")))
                .totalReturn(toBigDecimal(performance.get("totalReturn")))
                .maxDrawdown(toBigDecimal(performance.get("maxDrawdown")))
                .averageTrade(toBigDecimal(performance.get("averageTrade")))
                .averageSL(toBigDecimal(performance.get("averageSL")))
                .averageTP(toBigDecimal(performance.get("averageTP")))
                .rrMoyen(toBigDecimal(performance.get("RRmoyen")))
                .totalNetReturn(toBigDecimal(performance.get("totalNetReturn")))
                .netWinCount(toBigDecimal(performance.get("netWinCount")))
                .netLossCount(toBigDecimal(performance.get("netLossCount")))
                .averageNetTrade(toBigDecimal(performance.get("averageNetTrade")))
                .build();

        performanceRepository.save(perf);
    }

    public List<PerfsStratsDTO> getAllStrategyPerformances() {
        List<Performance> performances = performanceRepository.findAll();

        return performances.stream()
                .filter(perf -> perf.getMetric() == null)
                .map(this::mapPerformanceToPerfsStratsDTO)
                .toList();
    }

    private PerfsStratsDTO mapPerformanceToPerfsStratsDTO(Performance p) {
        return PerfsStratsDTO.builder()
                .name(p.getStrategyName())
                .runId(p.getRunId())
                .winCount(toBigDecimal(p.getWinCount()))
                .lossCount(toBigDecimal(p.getLossCount()))
                .totalReturn(toBigDecimal(p.getTotalReturn()))
                .maxDrawdown(toBigDecimal(p.getMaxDrawdown()))
                .averageTrade(toBigDecimal(p.getAverageTrade()))
                .averageSL(toBigDecimal(p.getAverageSL()))
                .averageTP(toBigDecimal(p.getAverageTP()))
                .symbol(p.getSymbol())
                .comparedSymbol(p.getComparedSymbol())
                .startStrategy(p.getStartStrategy())
                .endStrategy(p.getEndStrategy())
                .rrMoyen(toBigDecimal(p.getRrMoyen()))
                .totalNetReturn(toBigDecimal(p.getTotalNetReturn()))
                .netWinCount(toBigDecimal(p.getNetWinCount()))
                .netLossCount(toBigDecimal(p.getNetLossCount()))
                .averageNetTrade(toBigDecimal(p.getAverageNetTrade()))
                .timeframe(p.getTimeframe())
                .build();
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    private BigDecimal toBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
