package finance.project.api.runs;

import finance.project.api.config.PythonDispatchProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestTemplatePythonAsyncClientTest {

    @Test
    void sendsRequestIdHeaderAndReturnsJobId() {
        PythonDispatchProperties props = new PythonDispatchProperties();
        props.setBaseUrl("http://python.local");

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(once(), requestTo("http://python.local/submit/async"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Request-Id", "run-1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"spec_type\":\"backtest\"}"))
                .andRespond(withSuccess("{\"jobId\":\"py_123\"}", MediaType.APPLICATION_JSON));

        PythonAsyncClient client = new RestTemplatePythonAsyncClient(restTemplate, props);
        String jobId = client.submitAsync("run-1", "{\"spec_type\":\"backtest\"}");

        assertEquals("py_123", jobId);
        server.verify();
    }
}

