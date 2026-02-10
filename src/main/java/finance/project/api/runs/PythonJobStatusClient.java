package finance.project.api.runs;

public interface PythonJobStatusClient {
    PythonJobStatus getStatus(String jobId);
}

