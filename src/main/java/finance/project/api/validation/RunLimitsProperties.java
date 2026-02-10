package finance.project.api.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "run.limits")
public class RunLimitsProperties {
    private int maxStressTestSims = 5000;
    private int maxDateRangeDays = 3650;
    private int previewTimeoutMillis = 200;

    public int getMaxStressTestSims() {
        return maxStressTestSims;
    }

    public void setMaxStressTestSims(int maxStressTestSims) {
        this.maxStressTestSims = maxStressTestSims;
    }

    public int getMaxDateRangeDays() {
        return maxDateRangeDays;
    }

    public void setMaxDateRangeDays(int maxDateRangeDays) {
        this.maxDateRangeDays = maxDateRangeDays;
    }

    public int getPreviewTimeoutMillis() {
        return previewTimeoutMillis;
    }

    public void setPreviewTimeoutMillis(int previewTimeoutMillis) {
        this.previewTimeoutMillis = previewTimeoutMillis;
    }
}

