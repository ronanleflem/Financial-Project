package finance.project.api.runs;

import finance.project.api.config.PythonDispatchProperties;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplatePythonJobStatusClient implements PythonJobStatusClient {
    private final RestTemplate restTemplate;
    private final PythonDispatchProperties props;

    public RestTemplatePythonJobStatusClient(RestTemplateBuilder builder, PythonDispatchProperties props) {
        this.props = props;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMillis()))
                .setReadTimeout(Duration.ofMillis(props.getReadTimeoutMillis()))
                .build();
    }

    @Override
    public PythonJobStatus getStatus(String jobId) {
        try {
            URI uri = URI.create(trimTrailingSlash(props.getBaseUrl()) + "/status/" + jobId);
            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            Object body = response.getBody();
            if (body instanceof Map<?, ?> map) {
                Object status = map.get("status");
                if (status == null) {
                    status = map.get("state");
                }
                if (status != null) {
                    return parseStatus(String.valueOf(status));
                }
            }
            return PythonJobStatus.UNKNOWN;
        } catch (RestClientException ex) {
            throw PythonDispatchException.fromRestClientException(ex);
        }
    }

    private static PythonJobStatus parseStatus(String raw) {
        if (raw == null) {
            return PythonJobStatus.UNKNOWN;
        }
        return switch (raw.trim().toUpperCase()) {
            case "QUEUED" -> PythonJobStatus.QUEUED;
            case "RUNNING" -> PythonJobStatus.RUNNING;
            case "FAILED" -> PythonJobStatus.FAILED;
            case "DONE" -> PythonJobStatus.DONE;
            default -> PythonJobStatus.UNKNOWN;
        };
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

