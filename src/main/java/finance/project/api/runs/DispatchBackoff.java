package finance.project.api.runs;

public interface DispatchBackoff {
    void backoff(int attempt);
}

