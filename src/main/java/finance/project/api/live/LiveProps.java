package finance.project.api.live;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.live")
public record LiveProps(Ingest ingest, Sse sse) {

  public record Ingest(boolean hmacEnabled, String hmacHeader, String hmacSecretEnv) {}

  public record Sse(long heartbeatMillis) {}
}
