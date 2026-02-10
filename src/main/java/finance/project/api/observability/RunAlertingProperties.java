package finance.project.api.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "observability.alerts")
public class RunAlertingProperties {
    private boolean enabled = false;
    private int runsFailedPerMinuteThreshold = 10;
    private int dispatchFailedPerMinuteThreshold = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRunsFailedPerMinuteThreshold() {
        return runsFailedPerMinuteThreshold;
    }

    public void setRunsFailedPerMinuteThreshold(int runsFailedPerMinuteThreshold) {
        this.runsFailedPerMinuteThreshold = runsFailedPerMinuteThreshold;
    }

    public int getDispatchFailedPerMinuteThreshold() {
        return dispatchFailedPerMinuteThreshold;
    }

    public void setDispatchFailedPerMinuteThreshold(int dispatchFailedPerMinuteThreshold) {
        this.dispatchFailedPerMinuteThreshold = dispatchFailedPerMinuteThreshold;
    }
}

