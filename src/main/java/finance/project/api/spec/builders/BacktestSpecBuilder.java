package finance.project.api.spec.builders;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.BacktestDataBlock;
import finance.project.api.model.run.BacktestDynamicSlBlock;
import finance.project.api.model.run.BacktestJitterBlock;
import finance.project.api.model.run.BacktestScreeningBlock;
import finance.project.api.model.run.BacktestScreeningWindow;
import finance.project.api.model.run.BacktestSignalBlock;
import finance.project.api.model.run.BacktestStrategyBlock;
import finance.project.api.model.run.BacktestTpSlBlock;
import finance.project.api.model.run.FilterRuleSpec;
import finance.project.api.model.run.FilterSpec;
import finance.project.api.model.run.FiltersBlock;
import finance.project.api.model.run.PerformanceBlock;
import finance.project.api.model.run.PersistenceSpec;
import finance.project.api.model.run.RulesConfig;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.InvalidSpecTypeException;
import finance.project.api.spec.PythonSpecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BacktestSpecBuilder implements PythonSpecBuilder {
    private final StressTestsBuilder stressTestsBuilder;

    public BacktestSpecBuilder() {
        this(new StressTestsBuilder());
    }

    public BacktestSpecBuilder(StressTestsBuilder stressTestsBuilder) {
        this.stressTestsBuilder = stressTestsBuilder;
    }

    @Override
    public PythonSpec build(RunRequestInput input) {
        if (!"backtest".equals(input.specType())) {
            throw new InvalidSpecTypeException(input.specType());
        }

        BacktestDataBlock data = (BacktestDataBlock) input.data();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", buildData(data));

        Map<String, Object> signal = buildSignal(input.signal());
        if (!signal.isEmpty()) {
            payload.put("signal", signal);
        }

        BacktestStrategyBlock strategyBlock = (BacktestStrategyBlock) input.strategy();
        Map<String, Object> strategy = buildStrategy(data, strategyBlock);
        if (!strategy.isEmpty()) {
            payload.put("strategy", strategy);
        }

        FiltersBlock filters = input.filters();
        if (filters != null) {
            addIfNotEmpty(payload, "filters", buildFilters(filters.filters()));
            addIfNotEmpty(payload, "filter_rules", buildFilterRules(filters.rules()));
            addIfNotEmpty(payload, "filter_rules_config", buildRulesConfig(filters.rulesConfig()));
        }

        Map<String, Object> performance = buildPerformance(input.performance());
        if (!performance.isEmpty()) {
            payload.put("performance", performance);
        }

        if (input.output() != null && !input.output().isEmpty()) {
            payload.put("output", input.output());
        }

        Map<String, Object> persistence = buildPersistence(input.persistence());
        if (!persistence.isEmpty()) {
            payload.put("persistence", persistence);
        }

        return new PythonSpec(input.specType(), payload);
    }

    @Override
    public String supportsSpecType() {
        return "backtest";
    }

    private static Map<String, Object> buildData(BacktestDataBlock data) {
        Map<String, Object> dataSpec = new LinkedHashMap<>();
        dataSpec.put("symbols", List.of(data.symbol()));
        dataSpec.put("timeframe", data.timeframe());
        dataSpec.put("start", data.startDate());
        dataSpec.put("end", data.endDate());
        return dataSpec;
    }

    private static Map<String, Object> buildSignal(BacktestSignalBlock signal) {
        if (signal == null) {
            return Map.of();
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fast", signal.fast());
        params.put("slow", signal.slow());
        params.put("require_crossing", signal.requireCrossing());

        Map<String, Object> signalSpec = new LinkedHashMap<>();
        signalSpec.put("type", signal.type());
        signalSpec.put("params", params);
        return signalSpec;
    }

    private static Map<String, Object> buildStrategy(BacktestDataBlock data, BacktestStrategyBlock strategy) {
        Map<String, Object> strategySpec = new LinkedHashMap<>();
        String strategyId = strategy != null ? strategy.name() : null;
        if (strategyId == null) {
            strategyId = data.strategyName();
        }
        addIfNotNull(strategySpec, "strategy_id", strategyId);

        BacktestTpSlBlock tpSl = strategy != null ? strategy.tpSl() : null;
        if (tpSl != null) {
            Map<String, Object> tpSlSpec = buildTpSl(tpSl);
            addIfNotEmpty(strategySpec, "tp_sl", tpSlSpec);
        }

        Map<String, Object> screening = buildScreening(strategy);
        addIfNotEmpty(strategySpec, "screening", screening);

        return strategySpec;
    }

    private static Map<String, Object> buildTpSl(BacktestTpSlBlock tpSl) {
        Map<String, Object> tpSlSpec = new LinkedHashMap<>();
        addIfNotNull(tpSlSpec, "atr_window", tpSl.atrWindow());
        addIfNotNull(tpSlSpec, "atr_k", tpSl.atrK());
        addIfNotNull(tpSlSpec, "r_mult", tpSl.rMult());
        addIfNotNull(tpSlSpec, "slippage_bps", tpSl.slippageBps());
        addIfNotNull(tpSlSpec, "fee_bps", tpSl.feeBps());
        addIfNotNull(tpSlSpec, "sl_pct", tpSl.stopLossPct());
        addIfNotNull(tpSlSpec, "tp_pct", tpSl.takeProfitPct());
        addIfNotNull(tpSlSpec, "trailing_stop", tpSl.trailingStop());

        Map<String, Object> dynamicSl = buildDynamicSl(tpSl.dynamicSl());
        addIfNotEmpty(tpSlSpec, "dynamic_sl", dynamicSl);

        Map<String, Object> jitter = buildJitter(tpSl.jitter());
        if (!jitter.isEmpty()) {
            Map<String, Object> tpslSpec = new LinkedHashMap<>();
            tpslSpec.put("jitter", jitter);
            tpSlSpec.put("tpsl", tpslSpec);
        }

        return tpSlSpec;
    }

    private static Map<String, Object> buildDynamicSl(BacktestDynamicSlBlock dynamicSl) {
        if (dynamicSl == null) {
            return Map.of();
        }
        Map<String, Object> dynamicSlSpec = new LinkedHashMap<>();
        addIfNotNull(dynamicSlSpec, "enabled", dynamicSl.enabled());
        addIfNotNull(dynamicSlSpec, "mode", dynamicSl.mode());
        addIfNotNull(dynamicSlSpec, "atr_mult", dynamicSl.atrMult());
        return dynamicSlSpec;
    }

    private static Map<String, Object> buildJitter(BacktestJitterBlock jitter) {
        if (jitter == null) {
            return Map.of();
        }
        Map<String, Object> jitterSpec = new LinkedHashMap<>();
        addIfNotNull(jitterSpec, "enabled", jitter.enabled());
        addIfNotNull(jitterSpec, "dist", jitter.dist());
        addIfNotNull(jitterSpec, "tp_bps", jitter.tpBps());
        addIfNotNull(jitterSpec, "sl_bps", jitter.slBps());
        addIfNotNull(jitterSpec, "seed", jitter.seed());
        return jitterSpec;
    }

    private static List<Map<String, Object>> buildFilters(List<FilterSpec> filters) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (FilterSpec filter : filters) {
            if (filter == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            addIfNotNull(entry, "type", filter.id());
            if (filter.params() != null && !filter.params().isEmpty()) {
                entry.put("params", filter.params());
            }
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static List<Map<String, Object>> buildFilterRules(List<FilterRuleSpec> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (FilterRuleSpec rule : rules) {
            if (rule == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            addIfNotNull(entry, "id", rule.id());
            addIfNotNull(entry, "mode", rule.mode());
            addIfNotNull(entry, "weight", rule.weight());
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Map<String, Object> buildRulesConfig(RulesConfig config) {
        if (config == null) {
            return Map.of();
        }
        Map<String, Object> configSpec = new LinkedHashMap<>();
        addIfNotNull(configSpec, "min_score", config.minScore());
        addIfNotNull(configSpec, "min_score_pct", config.minScorePct());
        return configSpec;
    }

    private static Map<String, Object> buildScreening(BacktestStrategyBlock strategy) {
        BacktestScreeningBlock screening = strategy != null ? strategy.screening() : null;
        if (screening == null) {
            return Map.of();
        }
        Map<String, Object> screeningSpec = new LinkedHashMap<>();
        addIfNotNull(screeningSpec, "enabled", screening.enabled());
        BacktestScreeningWindow window = screening.window();
        if (window != null) {
            addIfNotNull(screeningSpec, "window_start", window.startDate());
            addIfNotNull(screeningSpec, "window_end", window.endDate());
        }
        addIfNotNull(screeningSpec, "max_bars", screening.maxBars());
        addIfNotNull(screeningSpec, "max_trades", screening.maxTrades());
        addIfNotNull(screeningSpec, "max_seconds", screening.maxSeconds());
        return screeningSpec;
    }

    private Map<String, Object> buildPerformance(PerformanceBlock performance) {
        if (performance == null) {
            return Map.of();
        }
        Map<String, Object> performanceSpec = new LinkedHashMap<>();
        addIfNotNull(performanceSpec, "initial_capital", performance.initialCapital());
        addIfNotNull(performanceSpec, "risk_pct", performance.riskPct());
        addIfNotNull(performanceSpec, "risk_free_rate_pct", performance.riskFreeRatePct());

        Map<String, Object> stressTests = stressTestsBuilder.build(performance.stressTests());
        addIfNotEmpty(performanceSpec, "stress_tests", stressTests);

        return performanceSpec;
    }

    private static Map<String, Object> buildPersistence(PersistenceSpec persistence) {
        if (persistence == null) {
            return Map.of();
        }
        Map<String, Object> persistenceSpec = new LinkedHashMap<>();
        addIfNotNull(persistenceSpec, "enabled", persistence.enabled());
        addIfNotNull(persistenceSpec, "spec_id", persistence.specId());
        addIfNotNull(persistenceSpec, "dataset_id", persistence.datasetId());
        return persistenceSpec;
    }

    private static void addIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static void addIfNotEmpty(Map<String, Object> target, String key, Map<String, Object> value) {
        if (value != null && !value.isEmpty()) {
            target.put(key, value);
        }
    }

    private static void addIfNotEmpty(Map<String, Object> target, String key, List<?> value) {
        if (value != null && !value.isEmpty()) {
            target.put(key, value);
        }
    }
}
