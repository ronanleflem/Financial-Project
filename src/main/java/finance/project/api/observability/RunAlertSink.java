package finance.project.api.observability;

public interface RunAlertSink {
    void alert(String type, String message);
}

