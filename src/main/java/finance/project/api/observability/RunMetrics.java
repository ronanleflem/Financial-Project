package finance.project.api.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RunMetrics {
    private final MeterRegistry registry;
    private final RunAlertingService alertingService;

    public RunMetrics(Optional<MeterRegistry> registry,
                      RunAlertingService alertingService) {
        this.registry = registry.orElse(null);
        this.alertingService = alertingService;
    }

    public void incrementRunsCreated(String specType) {
        alertingService.onRunCreated(specType);
        if (registry == null) {
            return;
        }
        Counter.builder("runs_created_total")
                .tags(Tags.of("specType", safeTag(specType)))
                .register(registry)
                .increment();
    }

    public void incrementRunsFailed(String reason) {
        alertingService.onRunFailed(reason);
        if (registry == null) {
            return;
        }
        Counter.builder("runs_failed_total")
                .tags(Tags.of("reason", safeTag(reason)))
                .register(registry)
                .increment();
    }

    public void incrementDispatchFailed() {
        alertingService.onDispatchFailed();
        if (registry == null) {
            return;
        }
        Counter.builder("dispatch_failed_total")
                .register(registry)
                .increment();
    }

    public void recordPreviewLatencyMillis(long latencyMillis) {
        recordTimerMillis("runs_preview_latency_ms", latencyMillis);
    }

    public void recordDispatchLatencyMillis(long latencyMillis) {
        recordTimerMillis("runs_dispatch_latency_ms", latencyMillis);
    }

    public void recordStatusLatencyMillis(long latencyMillis) {
        recordTimerMillis("runs_status_latency_ms", latencyMillis);
    }

    private void recordTimerMillis(String name, long latencyMillis) {
        if (registry == null) {
            return;
        }
        Timer.builder(name)
                .publishPercentileHistogram()
                .register(registry)
                .record(Duration.ofMillis(latencyMillis));
    }

    private static String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }
}

