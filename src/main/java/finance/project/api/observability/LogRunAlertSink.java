package finance.project.api.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogRunAlertSink implements RunAlertSink {
    private static final Logger log = LoggerFactory.getLogger(LogRunAlertSink.class);

    @Override
    public void alert(String type, String message) {
        log.warn("alert type={} message={}", type, message);
    }
}

