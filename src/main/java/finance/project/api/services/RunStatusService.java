package finance.project.api.services;

import finance.project.api.entities.run.RunExecutionEntity;
import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.entities.run.RunRequestEntity;
import finance.project.api.model.RunStatusResponse;
import finance.project.api.repositories.RunExecutionRepository;
import finance.project.api.repositories.RunRequestRepository;
import finance.project.api.runs.PythonDispatchException;
import finance.project.api.runs.PythonJobStatus;
import finance.project.api.runs.PythonJobStatusClient;
import org.springframework.stereotype.Service;

@Service
public class RunStatusService {
    private final RunRequestRepository runRequestRepository;
    private final RunExecutionRepository runExecutionRepository;
    private final PythonJobStatusClient pythonJobStatusClient;

    public RunStatusService(RunRequestRepository runRequestRepository,
                            RunExecutionRepository runExecutionRepository,
                            PythonJobStatusClient pythonJobStatusClient) {
        this.runRequestRepository = runRequestRepository;
        this.runExecutionRepository = runExecutionRepository;
        this.pythonJobStatusClient = pythonJobStatusClient;
    }

    public RunStatusResponse getStatus(String requestId) {
        String normalizedId = normalizeRequestId(requestId);
        RunRequestEntity runRequest = runRequestRepository.findByRequestId(normalizedId)
                .orElseThrow(() -> new RunRequestNotFoundException(normalizedId));

        RunExecutionEntity execution = runExecutionRepository.findByRequestId(runRequest.getRequestId()).orElse(null);

        PythonJobStatus pythonStatus = null;
        String message = baseMessage(runRequest.getStatus(), execution);
        if (execution != null && execution.getPythonJobId() != null && !execution.getPythonJobId().isBlank()) {
            try {
                pythonStatus = pythonJobStatusClient.getStatus(execution.getPythonJobId());
            } catch (PythonDispatchException ex) {
                if (message == null) {
                    message = "Python status unavailable";
                }
            }
        }

        RunStatusResponse.Status normalized = normalize(runRequest.getStatus(), pythonStatus);
        return new RunStatusResponse(runRequest.getRequestId(), normalized, runRequest.getUpdatedAt(), message);
    }

    private static RunStatusResponse.Status normalize(RunLifecycleStatus javaStatus, PythonJobStatus pythonStatus) {
        if (javaStatus == null) {
            return RunStatusResponse.Status.FAILED;
        }
        if (javaStatus == RunLifecycleStatus.DONE) {
            return RunStatusResponse.Status.COMPLETED;
        }
        if (javaStatus == RunLifecycleStatus.FAILED) {
            return RunStatusResponse.Status.FAILED;
        }
        if (javaStatus == RunLifecycleStatus.DISPATCH_FAILED) {
            return RunStatusResponse.Status.FAILED;
        }
        if (javaStatus == RunLifecycleStatus.PENDING) {
            return RunStatusResponse.Status.PENDING;
        }
        if (javaStatus == RunLifecycleStatus.RUNNING) {
            if (pythonStatus == null) {
                return RunStatusResponse.Status.RUNNING;
            }
            return switch (pythonStatus) {
                case QUEUED, RUNNING, UNKNOWN -> RunStatusResponse.Status.RUNNING;
                case DONE -> RunStatusResponse.Status.COMPLETED;
                case FAILED -> RunStatusResponse.Status.FAILED;
            };
        }
        return RunStatusResponse.Status.RUNNING;
    }

    private static String baseMessage(RunLifecycleStatus status, RunExecutionEntity execution) {
        if (status == RunLifecycleStatus.DISPATCH_FAILED) {
            return execution == null ? null : execution.getLastDispatchError();
        }
        if (status == RunLifecycleStatus.FAILED) {
            return execution == null ? null : execution.getLastDispatchError();
        }
        return null;
    }

    private static String normalizeRequestId(String requestId) {
        if (requestId == null) {
            return "";
        }
        return requestId.trim();
    }
}
