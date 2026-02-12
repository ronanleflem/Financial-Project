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
        metrics.incrementCanonicalProxyCalls("POST /runs", "2xx");
        metrics.incrementCanonicalProxyTimeouts("POST /runs");
        metrics.incrementCanonicalProxyOutcome("POST /runs", 200);
        metrics.incrementCanonicalProxyOutcome("POST /runs", 504);
        metrics.recordCanonicalProxyLatencyMillis("POST /runs", 120);

        assertTrue(registry.find("runs_created_total").counter() != null);
        assertTrue(registry.find("runs_failed_total").counter() != null);
        assertTrue(registry.find("dispatch_failed_total").counter() != null);
        assertTrue(registry.find("runs_canonical_proxy_calls_total").counter() != null);
        assertTrue(registry.find("runs_canonical_proxy_timeouts_total").counter() != null);
        assertTrue(registry.find("runs_canonical_proxy_outcomes_total").counter() != null);
        assertTrue(registry.find("runs_canonical_proxy_latency_ms").timer() != null);
    }
}
