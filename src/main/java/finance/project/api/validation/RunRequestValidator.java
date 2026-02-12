package finance.project.api.validation;

import finance.project.api.model.ValidationErrorItem;
import finance.project.api.model.run.BacktestDataBlock;
import finance.project.api.model.run.BacktestSignalBlock;
import finance.project.api.model.run.DcaDataBlock;
import finance.project.api.model.run.MarketStatsBlock;
import finance.project.api.model.run.MonteCarloStressTests;
import finance.project.api.model.run.PerformanceBlock;
import finance.project.api.model.run.RunRequestInput;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Legacy business validator. Canonical mode delegates business validation to Python.
 * Target removal date: 2026-06-30.
 */
@Deprecated(forRemoval = true, since = "X-3")
@Component
@ConditionalOnProperty(name = "run.engine.mode", havingValue = "LEGACY")
public class RunRequestValidator {
    private static final Set<String> STRESS_TEST_METHODS = Set.of("monte_carlo", "block", "block_bootstrap");
    private final RunLimitsProperties limits;

    public RunRequestValidator(Optional<RunLimitsProperties> limits) {
        this.limits = limits.orElse(new RunLimitsProperties());
    }

    public List<ValidationErrorItem> validate(RunRequestInput input) {
        List<ValidationErrorItem> errors = new ArrayList<>();
        if (input == null) {
            errors.add(new ValidationErrorItem("request", "must not be null"));
            return errors;
        }

        validateDateRange(input, errors);
        validateBacktestSignal(input, errors);
        validateMarketStats(input, errors);
        validateStressTests(input, errors);

        return errors;
    }

    private void validateDateRange(RunRequestInput input, List<ValidationErrorItem> errors) {
        if (input.data() instanceof DcaDataBlock dca) {
            validateDateRange("data.startDate", dca.startDate(), dca.endDate(), errors);
        } else if (input.data() instanceof BacktestDataBlock backtest) {
            validateDateRange("data.startDate", backtest.startDate(), backtest.endDate(), errors);
        }
    }

    private void validateDateRange(String fieldPrefix, String startDate, String endDate,
                                   List<ValidationErrorItem> errors) {
        if (startDate == null || endDate == null) {
            return;
        }
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (start.isAfter(end)) {
                errors.add(new ValidationErrorItem(fieldPrefix, "must be before data.endDate"));
                return;
            }
            long days = ChronoUnit.DAYS.between(start, end);
            if (days > limits.getMaxDateRangeDays()) {
                errors.add(new ValidationErrorItem("data.endDate", "range must be <= " + limits.getMaxDateRangeDays() + " days"));
            }
        } catch (DateTimeParseException ignored) {
            // Bean validation should cover format if required.
        }
    }

    private void validateBacktestSignal(RunRequestInput input, List<ValidationErrorItem> errors) {
        if (!"backtest".equals(input.specType())) {
            return;
        }
        BacktestSignalBlock signal = input.signal();
        if (signal == null) {
            errors.add(new ValidationErrorItem("signal", "is required"));
            return;
        }
        if (!"ema_cross".equals(signal.type())) {
            return;
        }
        Integer fast = signal.fast();
        Integer slow = signal.slow();
        if (fast != null && fast < 1) {
            errors.add(new ValidationErrorItem("signal.params.fast", "must be >= 1"));
        }
        if (slow != null && slow < 2) {
            errors.add(new ValidationErrorItem("signal.params.slow", "must be >= 2"));
        }
        if (fast != null && slow != null && fast >= slow) {
            errors.add(new ValidationErrorItem("signal.params.fast", "must be < signal.params.slow"));
        }
    }

    private void validateMarketStats(RunRequestInput input, List<ValidationErrorItem> errors) {
        if (!"market_stats".equals(input.specType())) {
            return;
        }
        MarketStatsBlock stats = input.stats();
        if (stats == null) {
            errors.add(new ValidationErrorItem("stats", "is required"));
        }
    }

    private void validateStressTests(RunRequestInput input, List<ValidationErrorItem> errors) {
        PerformanceBlock performance = input.performance();
        if (performance == null) {
            return;
        }
        MonteCarloStressTests stressTests = performance.stressTests();
        if (stressTests == null || !Boolean.TRUE.equals(stressTests.enabled())) {
            return;
        }

        if (stressTests.nSims() == null || stressTests.nSims() <= 0) {
            errors.add(new ValidationErrorItem("performance.stressTests.nSims", "must be > 0"));
        } else if (stressTests.nSims() > limits.getMaxStressTestSims()) {
            errors.add(new ValidationErrorItem("performance.stressTests.nSims", "must be <= " + limits.getMaxStressTestSims()));
        }
        if (stressTests.method() == null || !STRESS_TEST_METHODS.contains(stressTests.method())) {
            errors.add(new ValidationErrorItem("performance.stressTests.method", "must be one of: monte_carlo, block, block_bootstrap"));
        }
        if (stressTests.seed() == null || stressTests.seed() < 0) {
            errors.add(new ValidationErrorItem("performance.stressTests.seed", "must be >= 0"));
        }

        if ("block".equals(stressTests.method()) || "block_bootstrap".equals(stressTests.method())) {
            if (stressTests.blockSize() == null || stressTests.blockSize() < 1) {
                errors.add(new ValidationErrorItem("performance.stressTests.blockSize", "must be >= 1"));
            }
        }
    }
}
