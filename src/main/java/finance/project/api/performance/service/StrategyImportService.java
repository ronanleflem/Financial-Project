package finance.project.api.performance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.Performance;
import finance.project.api.entities.TradeCompleted;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.performance.dto.PythonCompletedTradeDTO;
import finance.project.api.performance.dto.PythonRunPayloadDTO;
import finance.project.api.performance.dto.PythonStrategyRunDTO;
import finance.project.api.repositories.PerformanceRepository;
import finance.project.api.repositories.TradeCompletedRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class StrategyImportService {

    private final PerformanceRepository performanceRepository;
    private final TradeCompletedRepository tradeCompletedRepository;
    private final ObjectMapper objectMapper;

    public StrategyImportService(PerformanceRepository performanceRepository,
                                 TradeCompletedRepository tradeCompletedRepository,
                                 ObjectMapper objectMapper) {
        this.performanceRepository = performanceRepository;
        this.tradeCompletedRepository = tradeCompletedRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void importFromPython(PythonRunPayloadDTO payload) {
        PythonStrategyRunDTO r = payload.getRun();

        Performance perf = performanceRepository
                .findByStrategyNameAndRunIdAndSymbolAndTimeframeAndAssetClassAndUniverse(
                        r.getStrategyId(),
                        r.getRunId(),
                        r.getSymbol(),
                        r.getTimeframe(),
                        r.getAssetClass(),
                        r.getUniverse())
                .orElseGet(Performance::new);
        perf.setStrategyName(r.getStrategyId());
        perf.setRunId(r.getRunId());
        perf.setAssetClass(r.getAssetClass());
        perf.setUniverse(r.getUniverse());
        perf.setTimeframe(r.getTimeframe());
        perf.setSymbol(r.getSymbol());
        perf.setComparedSymbol(r.getComparedSymbol());
        perf.setStartStrategy(toLocalDateTime(r.getStartTsUtc()));
        perf.setEndStrategy(toLocalDateTime(r.getEndTsUtc()));

        perf.setWinCount(r.getWinCount());
        perf.setLossCount(r.getLossCount());
        perf.setTotalReturn(r.getTotalReturn());
        perf.setMaxDrawdown(r.getMaxDrawdown());
        perf.setAverageTrade(r.getAverageTrade());
        perf.setAverageSL(r.getAverageSL());
        perf.setAverageTP(r.getAverageTP());
        perf.setRrMoyen(r.getRrMoyen());
        perf.setTotalNetReturn(r.getTotalNetReturn());
        perf.setNetWinCount(r.getNetWinCount());
        perf.setNetLossCount(r.getNetLossCount());
        perf.setAverageNetTrade(r.getAverageNetTrade());

        perf.setInitialCapital(r.getInitialCapital());
        perf.setFinalCapital(r.getFinalCapital());
        perf.setReturnPct(r.getReturnPct());
        perf.setMaxDrawdownPct(r.getMaxDrawdownPct());
        perf.setVolatilityPct(r.getVolatilityPct());
        perf.setSharpe(r.getSharpe());
        perf.setSortino(r.getSortino());
        perf.setWinratePct(r.getWinratePct());

        perf.setExtraJson(toJson(r.getExtra()));

        perf.setMetric(null);
        perf.setValue(0.0);

        performanceRepository.save(perf);

        if (payload.getTrades() == null) {
            return;
        }

        for (PythonCompletedTradeDTO t : payload.getTrades()) {
            TradeCompleted tc = new TradeCompleted();
            tc.setStrategyName(t.getStrategyId());
            tc.setRunId(t.getRunId());
            tc.setSymbol(t.getSymbol());
            tc.setAssetClass(t.getAssetClass());
            tc.setCycleId(t.getCycleId());
            tc.setTradeType(parseTradeType(t.getSide()));
            tc.setEntryTimestamp(toLocalDateTime(t.getEntryTimeUtc()));
            tc.setExitTimestamp(toLocalDateTime(t.getExitTimeUtc()));
            tc.setEntryPrice(toDouble(t.getEntryPrice()));
            tc.setExitPrice(toDouble(t.getExitPrice()));
            tc.setQuantity(t.getQuantity());
            tc.setProfitOrLoss(toDouble(t.getGrossPnl()));
            tc.setPnlPct(t.getGrossPnlPct());
            tc.setMaxDrawdownPct(t.getMaxDdPct());
            tc.setMetaJson(toJson(t.getMeta()));

            tradeCompletedRepository.save(tc);
        }
    }

    private LocalDateTime toLocalDateTime(java.time.OffsetDateTime offsetDateTime) {
        return Optional.ofNullable(offsetDateTime)
                .map(java.time.OffsetDateTime::toLocalDateTime)
                .orElse(null);
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private TradeSignalDTO.TradeType parseTradeType(String side) {
        if (side == null) {
            return null;
        }
        try {
            return TradeSignalDTO.TradeType.valueOf(side.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize object", e);
        }
    }
}
