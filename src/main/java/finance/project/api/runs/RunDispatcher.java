package finance.project.api.runs;

public interface RunDispatcher {
    void enqueue(String requestId);
}
