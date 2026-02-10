package finance.project.api.runs;

import com.fasterxml.jackson.databind.JsonNode;
import finance.project.api.config.PythonDispatchProperties;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplatePythonJobResultClient implements PythonJobResultClient {
    private final RestTemplate restTemplate;
    private final PythonDispatchProperties props;

    public RestTemplatePythonJobResultClient(RestTemplateBuilder builder, PythonDispatchProperties props) {
        this.props = props;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMillis()))
                .setReadTimeout(Duration.ofMillis(props.getReadTimeoutMillis()))
                .build();
    }

    @Override
    public JsonNode getResult(String jobId) {
        try {
            URI uri = URI.create(trimTrailingSlash(props.getBaseUrl()) + "/result/" + jobId);
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(uri, JsonNode.class);
            JsonNode body = response.getBody();
            return body == null ? com.fasterxml.jackson.databind.node.NullNode.getInstance() : body;
        } catch (RestClientException ex) {
            throw PythonDispatchException.fromRestClientException(ex);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

