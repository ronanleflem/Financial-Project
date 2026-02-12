package finance.project.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RunEngineStartupLogger {
    private static final Logger log = LoggerFactory.getLogger(RunEngineStartupLogger.class);

    private final String runEngineMode;
    private final PythonDispatchProperties pythonDispatchProperties;

    public RunEngineStartupLogger(@Value("${run.engine.mode:PYTHON_CANONICAL}") String runEngineMode,
                                  PythonDispatchProperties pythonDispatchProperties) {
        this.runEngineMode = runEngineMode;
        this.pythonDispatchProperties = pythonDispatchProperties;
    }

    @PostConstruct
    public void logRunEngineMode() {
        log.info(
                "run_engine_mode_active mode={} pythonBaseUrl={} connectTimeoutMs={} readTimeoutMs={}",
                runEngineMode,
                pythonDispatchProperties.getBaseUrl(),
                pythonDispatchProperties.getConnectTimeoutMillis(),
                pythonDispatchProperties.getReadTimeoutMillis()
        );
    }
}
