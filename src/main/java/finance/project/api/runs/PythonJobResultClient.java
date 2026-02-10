package finance.project.api.runs;

import com.fasterxml.jackson.databind.JsonNode;

public interface PythonJobResultClient {
    JsonNode getResult(String jobId);
}

