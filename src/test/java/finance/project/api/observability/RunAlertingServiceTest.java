package finance.project.api.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunAlertingServiceTest {

    @Test
    void triggersAlertsWhenThresholdExceeded() {
        RunAlertingProperties props = new RunAlertingProperties();
        props.setEnabled(true);
        props.setRunsFailedPerMinuteThreshold(1);
        props.setDispatchFailedPerMinuteThreshold(1);

        List<String> alerts = new ArrayList<>();
        RunAlertSink sink = (type, message) -> alerts.add(type + ":" + message);

        RunAlertingService service = new RunAlertingService(props, sink);

        service.onRunFailed("python");
        service.onRunFailed("python");
        service.onDispatchFailed();
        service.onDispatchFailed();

        service.evaluateAndReset();

        assertEquals(2, alerts.size());
    }
}

