package finance.project.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ibkr")
public class IbkrProperties {

    /** Hostname of the TWS/Gateway instance. */
    private String host = "127.0.0.1";

    /** Port exposed by TWS/Gateway API. */
    private int port = 7497;

    /** Client identifier used when connecting to IBKR. */
    private int clientId = 42;

    /** Maximum time to wait for the connection handshake (milliseconds). */
    private long connectTimeoutMillis = 10_000L;

    /** Minimum delay between two consecutive API requests to respect pacing rules. */
    private long pacingMinIntervalMillis = 1_200L;

    /** Backoff applied when IB reports a pacing violation (milliseconds). */
    private long pacingViolationBackoffMillis = 5_000L;

    /** Default timeout for request/response round trips (milliseconds). */
    private long defaultRequestTimeoutMillis = 8_000L;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getPacingMinIntervalMillis() {
        return pacingMinIntervalMillis;
    }

    public void setPacingMinIntervalMillis(long pacingMinIntervalMillis) {
        this.pacingMinIntervalMillis = pacingMinIntervalMillis;
    }

    public long getPacingViolationBackoffMillis() {
        return pacingViolationBackoffMillis;
    }

    public void setPacingViolationBackoffMillis(long pacingViolationBackoffMillis) {
        this.pacingViolationBackoffMillis = pacingViolationBackoffMillis;
    }

    public long getDefaultRequestTimeoutMillis() {
        return defaultRequestTimeoutMillis;
    }

    public void setDefaultRequestTimeoutMillis(long defaultRequestTimeoutMillis) {
        this.defaultRequestTimeoutMillis = defaultRequestTimeoutMillis;
    }
}
