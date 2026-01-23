package finance.project.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dukascopy")
public class DukascopyProperties {

    /** JNLP endpoint for the JForex client (demo or live). */
    private String jnlpUrl = "http://platform.dukascopy.com/demo_3/jforex_3.jnlp";

    /** JForex username. */
    private String username;

    /** JForex password. */
    private String password;

    /** Maximum time to wait for connection (milliseconds). */
    private long connectTimeoutMillis = 15_000L;

    /** Maximum time to wait for historical fetch (milliseconds). */
    private long historyTimeoutMillis = 30_000L;

    /** Offer side used when requesting bars (BID or ASK). */
    private String offerSide = "BID";

    public String getJnlpUrl() {
        return jnlpUrl;
    }

    public void setJnlpUrl(String jnlpUrl) {
        this.jnlpUrl = jnlpUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getHistoryTimeoutMillis() {
        return historyTimeoutMillis;
    }

    public void setHistoryTimeoutMillis(long historyTimeoutMillis) {
        this.historyTimeoutMillis = historyTimeoutMillis;
    }

    public String getOfferSide() {
        return offerSide;
    }

    public void setOfferSide(String offerSide) {
        this.offerSide = offerSide;
    }
}
