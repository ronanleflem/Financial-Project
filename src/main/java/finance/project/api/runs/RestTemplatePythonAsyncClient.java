package finance.project.api.runs;

import finance.project.api.config.PythonDispatchProperties;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplatePythonAsyncClient implements PythonAsyncClient {
    private final RestTemplate restTemplate;
    private final PythonDispatchProperties props;

    @Autowired
    public RestTemplatePythonAsyncClient(RestTemplateBuilder builder, PythonDispatchProperties props) {
        this.props = props;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMillis()))
                .setReadTimeout(Duration.ofMillis(props.getReadTimeoutMillis()))
                .build();
    }

    RestTemplatePythonAsyncClient(RestTemplate restTemplate, PythonDispatchProperties props) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    @Override
    public String submitAsync(String requestId, String specJson) {
        try {
            URI uri = URI.create(trimTrailingSlash(props.getBaseUrl()) + "/submit/async");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-Request-Id", requestId);
            HttpEntity<String> entity = new HttpEntity<>(specJson, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(uri, entity, Map.class);
            Object body = response.getBody();
            if (body instanceof Map<?, ?> map) {
                Object jobId = map.get("jobId");
                if (jobId != null) {
                    return String.valueOf(jobId);
                }
            }
            throw new PythonDispatchException("Missing jobId in response", response.getStatusCode().value(), false);
        } catch (PythonDispatchException e) {
            throw e;
        } catch (RestClientException e) {
            throw PythonDispatchException.fromRestClientException(e);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
