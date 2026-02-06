package finance.project.api.spec.builders;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.ValidationErrorItem;
import finance.project.api.model.run.CryptoGridParams;
import finance.project.api.model.run.DcaDataBlock;
import finance.project.api.model.run.DcaEquityParams;
import finance.project.api.model.run.DcaEtfParams;
import finance.project.api.model.run.DcaStrategyCore;
import finance.project.api.model.run.DcaStrategyType;
import finance.project.api.model.run.FilterRuleSpec;
import finance.project.api.model.run.FilterSpec;
import finance.project.api.model.run.FiltersBlock;
import finance.project.api.model.run.MonteCarloStressTests;
import finance.project.api.model.run.PerformanceBlock;
import finance.project.api.model.run.RulesConfig;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.StressOutputSpec;
import finance.project.api.model.run.StressScenarioSpec;
import finance.project.api.spec.InvalidSpecTypeException;
import finance.project.api.spec.PythonSpecBuilder;
import finance.project.api.validation.RunRequestValidationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StrategyBacktestSpecBuilder implements PythonSpecBuilder {

    @Override
    public PythonSpec build(RunRequestInput input) {
        if (!"dca".equals(input.specType())) {
            throw new InvalidSpecTypeException(input.specType());
        }

        DcaDataBlock data = (DcaDataBlock) input.data();
        DcaStrategyCore strategy = (DcaStrategyCore) input.strategy();
        validate(strategy);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", buildData(data));
        payload.put("strategy", buildStrategy(strategy));

        FiltersBlock filters = input.filters();
        if (filters != null) {
            payload.put("filters", buildFilters(filters.filters()));
            payload.put("filter_rules", buildFilterRules(filters.rules()));
            if (filters.rulesConfig() != null) {
                payload.put("filter_rules_config", buildRulesConfig(filters.rulesConfig()));
            }
        }

        Map<String, Object> performance = buildPerformance(input.performance());
        if (!performance.isEmpty()) {
            payload.put("performance", performance);
        }

        return new PythonSpec(input.specType(), payload);
    }

    @Override
    public String supportsSpecType() {
        return "dca";
    }

    private static Map<String, Object> buildData(DcaDataBlock data) {
        Map<String, Object> dataSpec = new LinkedHashMap<>();
        dataSpec.put("symbols", List.of(data.symbol()));
        dataSpec.put("timeframe", data.timeframe());
        dataSpec.put("start", data.startDate());
        dataSpec.put("end", data.endDate());
        dataSpec.put("frequency", data.frequency());
        dataSpec.put("amount", data.amount());
        return dataSpec;
    }

    private static Map<String, Object> buildStrategy(DcaStrategyCore strategy) {
        Map<String, Object> strategySpec = new LinkedHashMap<>();
        DcaStrategyType type = strategy.type();
        strategySpec.put("type", type.getValue());
        strategySpec.put("grid", strategy.grid());
        strategySpec.put("params", buildStrategyParams(type, strategy.params()));
        return strategySpec;
    }

    private static Map<String, Object> buildStrategyParams(DcaStrategyType type, Object params) {
        Map<String, Object> paramsSpec = new LinkedHashMap<>();
        if (type == null || params == null) {
            return paramsSpec;
        }

        switch (type) {
            case DCA_EQUITY -> {
                DcaEquityParams equity = (DcaEquityParams) params;
                addIfNotNull(paramsSpec, "drawdown_reference", equity.drawdownReference());
                addIfNotNull(paramsSpec, "execution_mode", equity.executionMode());
                addIfNotNull(paramsSpec, "tp_sl", equity.tpSlPreset());
                addIfNotNull(paramsSpec, "require_crossing", equity.requireCrossing());
            }
            case DCA_ETF -> {
                DcaEtfParams etf = (DcaEtfParams) params;
                addIfNotNull(paramsSpec, "activation_limit", etf.activationLimit());
                addIfNotNull(paramsSpec, "reset_on_new_high", etf.resetOnNewHigh());
                addIfNotNull(paramsSpec, "rearm_on_rebound_pct", etf.rearmOnReboundPct());
                addIfNotNull(paramsSpec, "force_close_end", etf.forceCloseEnd());
            }
            case CRYPTO_GRID -> {
                CryptoGridParams grid = (CryptoGridParams) params;
                addIfNotNull(paramsSpec, "tp_sl", grid.tpSlPreset());
            }
        }

        return paramsSpec;
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
        addIfNotNull(configSpec, "min_score", normalizeNumber(config.minScore()));
        addIfNotNull(configSpec, "min_score_pct", config.minScorePct());
        return configSpec;
    }

    private static Map<String, Object> buildPerformance(PerformanceBlock performance) {
        if (performance == null) {
            return Map.of();
        }

        Map<String, Object> performanceSpec = new LinkedHashMap<>();
        addIfNotNull(performanceSpec, "initial_capital", normalizeNumber(performance.initialCapital()));
        addIfNotNull(performanceSpec, "capital_per_unit", normalizeNumber(performance.capitalPerUnit()));
        addIfNotNull(performanceSpec, "max_capital_per_trade", normalizeNumber(performance.maxCapitalPerTrade()));

        Map<String, Object> stressTests = buildStressTests(performance.stressTests());
        addIfNotEmpty(performanceSpec, "stress_tests", stressTests);

        return performanceSpec;
    }

    private static Map<String, Object> buildStressTests(MonteCarloStressTests stressTests) {
        if (stressTests == null || !Boolean.TRUE.equals(stressTests.enabled())) {
            return Map.of();
        }

        Map<String, Object> stressTestsSpec = new LinkedHashMap<>();
        stressTestsSpec.put("enabled", true);

        Map<String, Object> monteCarlo = new LinkedHashMap<>();
        addIfNotNull(monteCarlo, "n_sims", stressTests.nSims());
        addIfNotNull(monteCarlo, "seed", stressTests.seed());
        addIfNotNull(monteCarlo, "method", stressTests.method());
        addIfNotEmpty(stressTestsSpec, "monte_carlo", monteCarlo);

        Map<String, Object> output = buildStressOutput(stressTests.output());
        addIfNotEmpty(stressTestsSpec, "output", output);

        if (stressTests.scenarios() != null) {
            stressTestsSpec.put("scenarios", buildStressScenarios(stressTests.scenarios()));
        }

        return stressTestsSpec;
    }

    private static Map<String, Object> buildStressOutput(StressOutputSpec output) {
        if (output == null) {
            return Map.of();
        }
        Map<String, Object> outputSpec = new LinkedHashMap<>();
        addIfNotNull(outputSpec, "mode", output.mode());
        return outputSpec;
    }

    private static List<Map<String, Object>> buildStressScenarios(List<StressScenarioSpec> scenarios) {
        if (scenarios == null) {
            return List.of();
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (StressScenarioSpec scenario : scenarios) {
            if (scenario == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            addIfNotNull(entry, "type", scenario.type());
            addIfNotNull(entry, "shock_pct", scenario.shockPct());
            addIfNotNull(entry, "window", scenario.window());
            addIfNotNull(entry, "index", scenario.index());
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static void validate(DcaStrategyCore strategy) {
        List<ValidationErrorItem> errors = new ArrayList<>();
        if (strategy == null) {
            errors.add(new ValidationErrorItem("strategy", "is required"));
            throw new RunRequestValidationException(errors);
        }

        if (strategy.grid() == null || strategy.grid().isEmpty()) {
            errors.add(new ValidationErrorItem("strategy.grid", "grid is required"));
        }

        DcaStrategyType type = strategy.type();
        if (type == null) {
            errors.add(new ValidationErrorItem("strategy.type", "is required"));
        } else {
            switch (type) {
                case DCA_EQUITY -> validateEquity(errors, (DcaEquityParams) strategy.params());
                case DCA_ETF -> validateEtf(errors, (DcaEtfParams) strategy.params());
                case CRYPTO_GRID -> validateCryptoGrid(errors, (CryptoGridParams) strategy.params());
            }
        }

        if (!errors.isEmpty()) {
            throw new RunRequestValidationException(errors);
        }
    }

    private static void validateEquity(List<ValidationErrorItem> errors, DcaEquityParams params) {
        if (params == null) {
            errors.add(new ValidationErrorItem("strategy.params", "is required"));
            return;
        }
        if (isBlank(params.drawdownReference())) {
            errors.add(new ValidationErrorItem("strategy.params.drawdown_reference", "is required"));
        }
        if (isBlank(params.executionMode())) {
            errors.add(new ValidationErrorItem("strategy.params.execution_mode", "is required"));
        }
        if (isBlank(params.tpSlPreset())) {
            errors.add(new ValidationErrorItem("strategy.params.tp_sl", "tp_sl is required"));
        }
    }

    private static void validateEtf(List<ValidationErrorItem> errors, DcaEtfParams params) {
        if (params == null) {
            errors.add(new ValidationErrorItem("strategy.params", "is required"));
            return;
        }
        if (params.activationLimit() == null) {
            errors.add(new ValidationErrorItem("strategy.params.activation_limit", "is required"));
        }
        if (params.rearmOnReboundPct() == null) {
            errors.add(new ValidationErrorItem("strategy.params.rearm_on_rebound_pct", "is required"));
        }
    }

    private static void validateCryptoGrid(List<ValidationErrorItem> errors, CryptoGridParams params) {
        if (params == null) {
            errors.add(new ValidationErrorItem("strategy.params", "is required"));
            return;
        }
        if (isBlank(params.tpSlPreset())) {
            errors.add(new ValidationErrorItem("strategy.params.tp_sl", "tp_sl is required"));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static Object normalizeNumber(Double value) {
        if (value == null) {
            return null;
        }
        if (value.isNaN() || value.isInfinite()) {
            return value;
        }
        if (value % 1 == 0) {
            return value.longValue();
        }
        return value;
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
}
