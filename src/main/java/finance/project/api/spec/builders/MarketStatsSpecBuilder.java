package finance.project.api.spec.builders;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.ValidationErrorItem;
import finance.project.api.model.run.MarketConditionSpec;
import finance.project.api.model.run.MarketEventSpec;
import finance.project.api.model.run.MarketStatsBlock;
import finance.project.api.model.run.MarketStatsDataBlock;
import finance.project.api.model.run.MarketTargetSpec;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.run.ValidationSpec;
import finance.project.api.spec.InvalidSpecTypeException;
import finance.project.api.spec.PythonSpecBuilder;
import finance.project.api.validation.RunRequestValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarketStatsSpecBuilder implements PythonSpecBuilder {
    @Override
    public PythonSpec build(RunRequestInput input) {
        if (!"market_stats".equals(input.specType())) {
            throw new InvalidSpecTypeException(input.specType());
        }

        MarketStatsDataBlock data = (MarketStatsDataBlock) input.data();
        MarketStatsBlock stats = input.stats();
        validate(data, stats);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", buildData(data));
        payload.put("stats", buildStats(stats));
        return new PythonSpec(input.specType(), payload);
    }

    @Override
    public String supportsSpecType() {
        return "market_stats";
    }

    private static Map<String, Object> buildData(MarketStatsDataBlock data) {
        Map<String, Object> dataSpec = new LinkedHashMap<>();
        dataSpec.put("symbols", List.of(data.symbol()));
        dataSpec.put("timeframe", data.timeframe());
        dataSpec.put("lookback", data.lookback());
        dataSpec.put("stats_pack", data.statsPack());
        dataSpec.put("session", data.session());
        dataSpec.put("include_weekends", data.includeWeekends());
        return dataSpec;
    }

    private static Map<String, Object> buildStats(MarketStatsBlock stats) {
        Map<String, Object> statsSpec = new LinkedHashMap<>();
        statsSpec.put("event", buildLeaf(stats.event()));
        statsSpec.put("condition", buildLeaf(stats.condition()));
        statsSpec.put("target", buildLeaf(stats.target()));
        Map<String, Object> validation = buildValidation(stats.validation());
        if (!validation.isEmpty()) {
            statsSpec.put("validation", validation);
        }
        return statsSpec;
    }

    private static Map<String, Object> buildLeaf(MarketEventSpec spec) {
        Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("id", spec.id());
        leaf.put("params", spec.params() == null ? Map.of() : spec.params());
        return leaf;
    }

    private static Map<String, Object> buildLeaf(MarketConditionSpec spec) {
        Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("id", spec.id());
        leaf.put("params", spec.params() == null ? Map.of() : spec.params());
        return leaf;
    }

    private static Map<String, Object> buildLeaf(MarketTargetSpec spec) {
        Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("id", spec.id());
        leaf.put("params", spec.params() == null ? Map.of() : spec.params());
        return leaf;
    }

    private static Map<String, Object> buildValidation(ValidationSpec validation) {
        if (validation == null) {
            return Map.of();
        }
        Map<String, Object> validationSpec = new LinkedHashMap<>();
        addIfNotNull(validationSpec, "train_months", validation.trainMonths());
        addIfNotNull(validationSpec, "test_months", validation.testMonths());
        addIfNotNull(validationSpec, "folds", validation.folds());
        addIfNotNull(validationSpec, "embargo_days", validation.embargoDays());
        return validationSpec;
    }

    private static void validate(MarketStatsDataBlock data, MarketStatsBlock stats) {
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
            Integer lookback = data.lookback();
            if (lookback != null && lookback < 100) {
                errors.add(new ValidationErrorItem("data.lookback", "must be >= 100"));
            }
        }

        if (stats == null) {
            errors.add(new ValidationErrorItem("stats", "is required"));
        } else {
            validateLeaf("stats.event.id", stats.event() != null ? stats.event().id() : null, errors);
            validateLeaf("stats.condition.id", stats.condition() != null ? stats.condition().id() : null, errors);
            validateLeaf("stats.target.id", stats.target() != null ? stats.target().id() : null, errors);

            ValidationSpec validation = stats.validation();
            if (validation != null) {
                validateMin("stats.validation.trainMonths", validation.trainMonths(), 1, errors);
                validateMin("stats.validation.testMonths", validation.testMonths(), 1, errors);
                validateMin("stats.validation.folds", validation.folds(), 1, errors);
                validateMin("stats.validation.embargoDays", validation.embargoDays(), 0, errors);
            }
        }

        if (!errors.isEmpty()) {
            throw new RunRequestValidationException(errors);
        }
    }

    private static void validateLeaf(String field, String id, List<ValidationErrorItem> errors) {
        if (isBlank(id)) {
            errors.add(new ValidationErrorItem(field, "is required"));
        }
    }

    private static void validateMin(String field, Integer value, int minInclusive, List<ValidationErrorItem> errors) {
        if (value == null) {
            errors.add(new ValidationErrorItem(field, "is required"));
            return;
        }
        if (value < minInclusive) {
            errors.add(new ValidationErrorItem(field, "must be >= " + minInclusive));
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
