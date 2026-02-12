package finance.project.api.spec.builders;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.ValidationErrorItem;
import finance.project.api.model.run.SeasonalityBlock;
import finance.project.api.model.run.SeasonalityCompute;
import finance.project.api.model.run.SeasonalityDataBlock;
import finance.project.api.model.run.SeasonalityExecution;
import finance.project.api.model.run.SeasonalityProfile;
import finance.project.api.model.run.SeasonalitySignal;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.InvalidSpecTypeException;
import finance.project.api.spec.PythonSpecBuilder;
import finance.project.api.validation.RunRequestValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Deprecated
@Component
@ConditionalOnProperty(name = "run.engine.mode", havingValue = "LEGACY")
public class SeasonalitySpecBuilder implements PythonSpecBuilder {
    private static final int DEFAULT_SIGNAL_TOPK = 3;
    private static final double DEFAULT_SIGNAL_THRESHOLD = 1.0;
    private static final int DEFAULT_COMPUTE_MAX_TRIALS = 50;
    private static final String DEFAULT_COMPUTE_SEARCH_SPACE = "default";
    private static final String DEFAULT_EXECUTION_RISK_MODEL = "fixed";
    private static final String DEFAULT_EXECUTION_TP_SL = "default";

    @Override
    public PythonSpec build(RunRequestInput input) {
        if (!"seasonality".equals(input.specType())) {
            throw new InvalidSpecTypeException(input.specType());
        }

        SeasonalityDataBlock data = (SeasonalityDataBlock) input.data();
        SeasonalityBlock seasonality = input.seasonality();
        validate(data, seasonality);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", buildData(data));

        SeasonalityProfile profile = seasonality.profile();
        payload.put("profile", buildProfile(profile));

        payload.put("signal", buildSignal(seasonality.signal()));
        payload.put("compute", buildCompute(seasonality.compute()));
        payload.put("execution", buildExecution(seasonality.execution()));

        if (seasonality.risk() != null && !seasonality.risk().isEmpty()) {
            payload.put("risk", seasonality.risk());
        }
        if (seasonality.tpSl() != null && !seasonality.tpSl().isEmpty()) {
            payload.put("tp_sl", seasonality.tpSl());
        }

        return new PythonSpec(input.specType(), payload);
    }

    @Override
    public String supportsSpecType() {
        return "seasonality";
    }

    private static Map<String, Object> buildData(SeasonalityDataBlock data) {
        Map<String, Object> dataSpec = new LinkedHashMap<>();
        dataSpec.put("symbols", List.of(data.symbol()));
        dataSpec.put("timeframe", data.timeframe());
        dataSpec.put("window", data.window());
        dataSpec.put("start_year", data.startYear());
        dataSpec.put("end_year", data.endYear());
        dataSpec.put("filter", data.filter());
        dataSpec.put("normalize", data.normalize());
        return dataSpec;
    }

    private static Map<String, Object> buildProfile(SeasonalityProfile profile) {
        Map<String, Object> profileSpec = new LinkedHashMap<>();
        profileSpec.put("id", profile.id());
        addIfNotNull(profileSpec, "measure", profile.measure());
        addIfNotNull(profileSpec, "ret_horizon", profile.retHorizon());
        addIfNotNull(profileSpec, "min_samples_bin", profile.minSamplesBin());
        profileSpec.put("params", profile.params() == null ? Map.of() : profile.params());
        return profileSpec;
    }

    private static Map<String, Object> buildSignal(SeasonalitySignal signal) {
        double threshold = signal.threshold() != null ? signal.threshold() : DEFAULT_SIGNAL_THRESHOLD;
        int topk = signal.topk() != null ? signal.topk() : DEFAULT_SIGNAL_TOPK;

        Map<String, Object> signalSpec = new LinkedHashMap<>();
        signalSpec.put("method", signal.method());
        signalSpec.put("threshold", threshold);
        signalSpec.put("topk", topk);
        signalSpec.put("dims", signal.dims() == null ? List.of() : signal.dims());
        addIfNotNull(signalSpec, "combine", signal.combine());
        return signalSpec;
    }

    private static Map<String, Object> buildCompute(SeasonalityCompute compute) {
        Integer maxTrials = compute != null ? compute.maxTrials() : null;
        String searchSpace = compute != null ? compute.searchSpace() : null;

        int resolvedMaxTrials = maxTrials != null ? maxTrials : DEFAULT_COMPUTE_MAX_TRIALS;
        String resolvedSearchSpace = isBlank(searchSpace) ? DEFAULT_COMPUTE_SEARCH_SPACE : searchSpace;

        Map<String, Object> computeSpec = new LinkedHashMap<>();
        computeSpec.put("max_trials", resolvedMaxTrials);
        computeSpec.put("search_space", resolvedSearchSpace);
        return computeSpec;
    }

    private static Map<String, Object> buildExecution(SeasonalityExecution execution) {
        String riskModel = execution != null ? execution.riskModel() : null;
        String tpSl = execution != null ? execution.tpSl() : null;

        Map<String, Object> executionSpec = new LinkedHashMap<>();
        executionSpec.put("risk_model", isBlank(riskModel) ? DEFAULT_EXECUTION_RISK_MODEL : riskModel);
        executionSpec.put("tp_sl", isBlank(tpSl) ? DEFAULT_EXECUTION_TP_SL : tpSl);
        return executionSpec;
    }

    private static void validate(SeasonalityDataBlock data, SeasonalityBlock seasonality) {
        List<ValidationErrorItem> errors = new java.util.ArrayList<>();

        if (data == null) {
            errors.add(new ValidationErrorItem("data", "is required"));
        } else {
            if (isBlank(data.symbol())) {
                errors.add(new ValidationErrorItem("data.symbol", "is required"));
            }
            if (isBlank(data.timeframe())) {
                errors.add(new ValidationErrorItem("data.timeframe", "is required"));
            }
            if (data.startYear() != null && data.endYear() != null && data.startYear() > data.endYear()) {
                errors.add(new ValidationErrorItem("data.startYear", "must be <= data.endYear"));
            }
        }

        if (seasonality == null) {
            errors.add(new ValidationErrorItem("seasonality", "is required"));
        } else {
            SeasonalityProfile profile = seasonality.profile();
            if (profile == null || isBlank(profile.id())) {
                errors.add(new ValidationErrorItem("seasonality.profile.id", "is required"));
            } else if (profile.minSamplesBin() != null && profile.minSamplesBin() < 10) {
                errors.add(new ValidationErrorItem("seasonality.profile.minSamplesBin", "must be >= 10"));
            }

            SeasonalitySignal signal = seasonality.signal();
            if (signal == null || isBlank(signal.method())) {
                errors.add(new ValidationErrorItem("seasonality.signal.method", "is required"));
            } else {
                int topk = signal.topk() != null ? signal.topk() : DEFAULT_SIGNAL_TOPK;
                if (topk < 1) {
                    errors.add(new ValidationErrorItem("seasonality.signal.topk", "must be >= 1"));
                }
            }

            int maxTrials = DEFAULT_COMPUTE_MAX_TRIALS;
            if (seasonality.compute() != null && seasonality.compute().maxTrials() != null) {
                maxTrials = seasonality.compute().maxTrials();
            }
            if (maxTrials < 1) {
                errors.add(new ValidationErrorItem("seasonality.compute.maxTrials", "must be >= 1"));
            }
        }

        if (!errors.isEmpty()) {
            throw new RunRequestValidationException(errors);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void addIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
