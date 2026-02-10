package finance.project.api.observability;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RunAlertingService {
    private final RunAlertingProperties props;
    private final RunAlertSink sink;

    private final AtomicInteger runsFailedThisMinute = new AtomicInteger(0);
    private final AtomicInteger dispatchFailedThisMinute = new AtomicInteger(0);

    public RunAlertingService(RunAlertingProperties props, RunAlertSink sink) {
        this.props = props;
        this.sink = sink;
    }

    void onRunCreated(String specType) {
        // placeholder for future alerting per spec type
    }

    void onRunFailed(String reason) {
        if (props.isEnabled()) {
            runsFailedThisMinute.incrementAndGet();
        }
    }

    void onDispatchFailed() {
        if (props.isEnabled()) {
            dispatchFailedThisMinute.incrementAndGet();
        }
    }

    @Scheduled(fixedRate = 60_000)
    public void evaluateAndReset() {
        if (!props.isEnabled()) {
            runsFailedThisMinute.set(0);
            dispatchFailedThisMinute.set(0);
            return;
        }

        int failed = runsFailedThisMinute.getAndSet(0);
        int dispatchFailed = dispatchFailedThisMinute.getAndSet(0);

        if (failed > props.getRunsFailedPerMinuteThreshold()) {
            sink.alert("runs_failed_total", "runs_failed_total=" + failed + " per minute exceeded threshold=" + props.getRunsFailedPerMinuteThreshold());
        }
        if (dispatchFailed > props.getDispatchFailedPerMinuteThreshold()) {
            sink.alert("dispatch_failed_total", "dispatch_failed_total=" + dispatchFailed + " per minute exceeded threshold=" + props.getDispatchFailedPerMinuteThreshold());
        }
    }
}

