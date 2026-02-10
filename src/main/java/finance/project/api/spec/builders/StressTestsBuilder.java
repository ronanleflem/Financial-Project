package finance.project.api.spec.builders;

import finance.project.api.model.ValidationErrorItem;
import finance.project.api.model.run.MonteCarloStressTests;
import finance.project.api.model.run.MultiAssetSpec;
import finance.project.api.model.run.ParamDriftSpec;
import finance.project.api.model.run.SizingSpec;
import finance.project.api.model.run.StressOutputSpec;
import finance.project.api.model.run.StressScenarioSpec;
import finance.project.api.model.run.TimeDistributionSpec;
import finance.project.api.validation.RunRequestValidationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StressTestsBuilder {
    private static final Set<String> METHODS = Set.of("monte_carlo", "block", "block_bootstrap");

    public Map<String, Object> build(MonteCarloStressTests stressTests) {
        return build(stressTests, "performance.stressTests");
    }

    public Map<String, Object> build(MonteCarloStressTests stressTests, String fieldPrefix) {
        if (stressTests == null || !Boolean.TRUE.equals(stressTests.enabled())) {
            return Map.of();
        }

        validate(stressTests, fieldPrefix);

        Map<String, Object> stressTestsSpec = new LinkedHashMap<>();
        stressTestsSpec.put("enabled", true);

        Map<String, Object> monteCarlo = new LinkedHashMap<>();
        monteCarlo.put("n_sims", stressTests.nSims());
        addIfNotNull(monteCarlo, "seed", stressTests.seed());
        addIfNotNull(monteCarlo, "method", stressTests.method());
        addIfNotNull(monteCarlo, "block_size", stressTests.blockSize());
        addIfNotNull(monteCarlo, "overlapping", stressTests.overlapping());
        stressTestsSpec.put("monte_carlo", monteCarlo);

        Map<String, Object> timeDistribution = buildTimeDistribution(stressTests.timeDistribution());
        addIfNotEmpty(stressTestsSpec, "time_distribution", timeDistribution);

        Map<String, Object> paramDrift = buildParamDrift(stressTests.paramDrift());
        addIfNotEmpty(stressTestsSpec, "param_drift", paramDrift);

        Map<String, Object> sizing = buildSizing(stressTests.sizing());
        addIfNotEmpty(stressTestsSpec, "sizing", sizing);

        Map<String, Object> output = buildOutput(stressTests.output());
        addIfNotEmpty(stressTestsSpec, "output", output);

        if (stressTests.scenarios() != null) {
            stressTestsSpec.put("scenarios", buildScenarios(stressTests.scenarios()));
        }

        Map<String, Object> multiAsset = buildMultiAsset(stressTests.multiAsset());
        addIfNotEmpty(stressTestsSpec, "multi_asset", multiAsset);

        return stressTestsSpec;
    }

    private static void validate(MonteCarloStressTests stressTests, String fieldPrefix) {
        List<ValidationErrorItem> errors = new ArrayList<>();

        if (isBlank(stressTests.method())) {
            errors.add(new ValidationErrorItem(fieldPrefix + ".method", "is required"));
        } else if (!METHODS.contains(stressTests.method())) {
            errors.add(new ValidationErrorItem(fieldPrefix + ".method", "must be one of: monte_carlo, block, block_bootstrap"));
        }

        if (stressTests.nSims() == null || stressTests.nSims() <= 0) {
            errors.add(new ValidationErrorItem(fieldPrefix + ".nSims", "must be > 0"));
        }

        if (stressTests.seed() != null && stressTests.seed() < 0) {
            errors.add(new ValidationErrorItem(fieldPrefix + ".seed", "must be >= 0"));
        }

        if ("block".equals(stressTests.method()) || "block_bootstrap".equals(stressTests.method())) {
            if (stressTests.blockSize() == null || stressTests.blockSize() < 1) {
                errors.add(new ValidationErrorItem(fieldPrefix + ".blockSize", "must be >= 1"));
            }
        }

        if (!errors.isEmpty()) {
            throw new RunRequestValidationException(errors);
        }
    }

    private static Map<String, Object> buildTimeDistribution(TimeDistributionSpec spec) {
        if (spec == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        addIfNotNull(out, "mode", spec.mode());
        addIfNotNull(out, "seed", spec.seed());
        return out;
    }

    private static Map<String, Object> buildParamDrift(ParamDriftSpec spec) {
        if (spec == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        addIfNotNull(out, "mode", spec.mode());
        addIfNotNull(out, "dist", spec.dist());
        addIfNotNull(out, "mu", spec.mu());
        addIfNotNull(out, "sigma", spec.sigma());
        return out;
    }

    private static Map<String, Object> buildSizing(SizingSpec spec) {
        if (spec == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        addIfNotNull(out, "dist", spec.dist());
        addIfNotNull(out, "mu", spec.mu());
        addIfNotNull(out, "sigma", spec.sigma());
        return out;
    }

    private static Map<String, Object> buildOutput(StressOutputSpec output) {
        if (output == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        addIfNotNull(out, "mode", output.mode());
        addIfNotNull(out, "max_curves", output.maxCurves());
        addIfNotNull(out, "curve_stride", output.curveStride());
        return out;
    }

    private static List<Map<String, Object>> buildScenarios(List<StressScenarioSpec> scenarios) {
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
            addIfNotNull(entry, "vol_multiplier", scenario.volMultiplier());
            addIfNotNull(entry, "drawdown_pct", scenario.drawdownPct());
            addIfNotNull(entry, "window", scenario.window());
            addIfNotNull(entry, "index", scenario.index());
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Map<String, Object> buildMultiAsset(MultiAssetSpec multiAsset) {
        if (multiAsset == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        addIfNotNull(out, "aggregation", multiAsset.aggregation());
        addIfNotNull(out, "weights", multiAsset.weights());
        addIfNotNull(out, "timestamp_alignment", multiAsset.timestampAlignment());
        return out;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

