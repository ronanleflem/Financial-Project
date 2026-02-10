package finance.project.api.observability;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RunMetricsTest {

    @Test
    void incrementsCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        RunAlertingProperties props = new RunAlertingProperties();
        props.setEnabled(false);
        RunAlertingService alertingService = new RunAlertingService(props, (type, message) -> {
        });

        RunMetrics metrics = new RunMetrics(Optional.of(registry), alertingService);
        metrics.incrementRunsCreated("backtest");
        metrics.incrementRunsFailed("dispatch");
        metrics.incrementDispatchFailed();

        assertTrue(registry.find("runs_created_total").counter() != null);
        assertTrue(registry.find("runs_failed_total").counter() != null);
        assertTrue(registry.find("dispatch_failed_total").counter() != null);
    }
}

