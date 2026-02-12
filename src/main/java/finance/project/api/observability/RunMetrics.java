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

    public void recordCanonicalProxyLatencyMillis(String endpoint, long latencyMillis) {
        if (registry == null) {
            return;
        }
        Timer.builder("runs_canonical_proxy_latency_ms")
                .tags(Tags.of("endpoint", safeTag(endpoint)))
                .publishPercentileHistogram()
                .publishPercentiles(0.95, 0.99)
                .register(registry)
                .record(Duration.ofMillis(latencyMillis));
    }

    public void incrementCanonicalProxyCalls(String endpoint, String statusFamily) {
        if (registry == null) {
            return;
        }
        Counter.builder("runs_canonical_proxy_calls_total")
                .tags(Tags.of(
                        "endpoint", safeTag(endpoint),
                        "statusFamily", safeTag(statusFamily)
                ))
                .register(registry)
                .increment();
    }

    public void incrementCanonicalProxyTimeouts(String endpoint) {
        if (registry == null) {
            return;
        }
        Counter.builder("runs_canonical_proxy_timeouts_total")
                .tags(Tags.of("endpoint", safeTag(endpoint)))
                .register(registry)
                .increment();
    }

    public void incrementCanonicalProxyOutcome(String endpoint, int httpStatus) {
        if (registry == null) {
            return;
        }
        Counter.builder("runs_canonical_proxy_outcomes_total")
                .tags(Tags.of(
                        "endpoint", safeTag(endpoint),
                        "outcome", outcome(httpStatus)
                ))
                .register(registry)
                .increment();
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

    private static String outcome(int httpStatus) {
        if (httpStatus >= 200 && httpStatus < 300) {
            return "success";
        }
        return "error";
    }
}
