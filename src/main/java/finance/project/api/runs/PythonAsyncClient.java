package finance.project.api.runs;

public interface PythonAsyncClient {
    String submitAsync(String requestId, String specJson);
}

