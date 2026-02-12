package finance.project.api.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.run.RunArtifactEntity;
import finance.project.api.entities.run.RunErrorEntity;
import finance.project.api.entities.run.RunExecutionEntity;
import finance.project.api.entities.run.RunLifecycleStatus;
import finance.project.api.entities.run.RunRequestEntity;
import finance.project.api.model.RunResultResponse;
import finance.project.api.model.RunStatusResponse;
import finance.project.api.repositories.RunArtifactRepository;
import finance.project.api.repositories.RunErrorRepository;
import finance.project.api.repositories.RunExecutionRepository;
import finance.project.api.repositories.RunRequestRepository;
import finance.project.api.runs.PythonDispatchException;
import finance.project.api.runs.PythonJobResultClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Deprecated
@Service
@ConditionalOnProperty(name = "run.engine.mode", havingValue = "LEGACY")
public class RunResultService {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final RunRequestRepository runRequestRepository;
    private final RunExecutionRepository runExecutionRepository;
    private final RunErrorRepository runErrorRepository;
    private final RunArtifactRepository runArtifactRepository;
    private final RunStatusService runStatusService;
    private final PythonJobResultClient pythonJobResultClient;
    private final ObjectMapper objectMapper;

    public RunResultService(RunRequestRepository runRequestRepository,
                            RunExecutionRepository runExecutionRepository,
                            RunErrorRepository runErrorRepository,
                            RunArtifactRepository runArtifactRepository,
                            RunStatusService runStatusService,
                            PythonJobResultClient pythonJobResultClient,
                            ObjectMapper objectMapper) {
        this.runRequestRepository = runRequestRepository;
        this.runExecutionRepository = runExecutionRepository;
        this.runErrorRepository = runErrorRepository;
        this.runArtifactRepository = runArtifactRepository;
        this.runStatusService = runStatusService;
        this.pythonJobResultClient = pythonJobResultClient;
        this.objectMapper = objectMapper;
    }

    public RunResultResponse getResult(String requestId) {
        String normalizedId = normalizeRequestId(requestId);
        RunRequestEntity runRequest = runRequestRepository.findByRequestId(normalizedId)
                .orElseThrow(() -> new RunRequestNotFoundException(normalizedId));

        RunExecutionEntity execution = runExecutionRepository.findByRequestId(runRequest.getRequestId()).orElse(null);

        RunStatusResponse.Status status = runStatusService.getStatus(runRequest.getRequestId()).status();
        if (status != RunStatusResponse.Status.COMPLETED && status != RunStatusResponse.Status.FAILED) {
            return new RunResultResponse(runRequest.getRequestId(), status, null, null, null, null, "Result not available yet");
        }

        if (status == RunStatusResponse.Status.FAILED) {
            return new RunResultResponse(
                    runRequest.getRequestId(),
                    status,
                    null,
                    null,
                    null,
                    buildError(runRequest, execution),
                    null
            );
        }

        StoredOrRemoteResult result = readStoredOrFetchRemote(execution);
        List<RunResultResponse.Artifact> artifacts = readArtifacts(runRequest.getRequestId());

        return new RunResultResponse(
                runRequest.getRequestId(),
                status,
                result.result(),
                artifacts,
                result.warnings(),
                null,
                null
        );
    }

    private List<RunResultResponse.Artifact> readArtifacts(String requestId) {
        List<RunArtifactEntity> stored = runArtifactRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
        if (stored.isEmpty()) {
            return null;
        }
        List<RunResultResponse.Artifact> artifacts = new ArrayList<>();
        for (RunArtifactEntity a : stored) {
            artifacts.add(new RunResultResponse.Artifact(
                    a.getType(),
                    a.getPath(),
                    parseJsonOrNull(a.getMeta())
            ));
        }
        return artifacts;
    }

    private StoredOrRemoteResult readStoredOrFetchRemote(RunExecutionEntity execution) {
        if (execution == null) {
            return new StoredOrRemoteResult(null, null);
        }

        JsonNode storedResult = parseJsonOrNull(execution.getResultJson());
        List<String> storedWarnings = parseWarningsOrNull(execution.getWarningsJson());
        if (storedResult != null || storedWarnings != null) {
            return new StoredOrRemoteResult(storedResult, storedWarnings);
        }

        if (execution.getPythonJobId() == null || execution.getPythonJobId().isBlank()) {
            return new StoredOrRemoteResult(null, null);
        }

        try {
            JsonNode remote = pythonJobResultClient.getResult(execution.getPythonJobId());
            return parseRemoteResult(remote);
        } catch (PythonDispatchException ex) {
            return new StoredOrRemoteResult(null, List.of("Python result unavailable"));
        }
    }

    private StoredOrRemoteResult parseRemoteResult(JsonNode remote) {
        if (remote == null || remote.isNull() || remote.isMissingNode()) {
            return new StoredOrRemoteResult(null, null);
        }

        JsonNode result = remote.get("result");
        List<String> warnings = null;

        JsonNode warningsNode = remote.get("warnings");
        if (warningsNode != null && warningsNode.isArray()) {
            warnings = new ArrayList<>();
            for (JsonNode n : warningsNode) {
                warnings.add(n.asText());
            }
        }

        return new StoredOrRemoteResult(result, warnings);
    }

    private RunResultResponse.Error buildError(RunRequestEntity runRequest, RunExecutionEntity execution) {
        List<RunErrorEntity> errors = runErrorRepository.findByRequestIdOrderByCreatedAtAsc(runRequest.getRequestId());
        RunErrorEntity lastPython = null;
        for (int i = errors.size() - 1; i >= 0; i--) {
            RunErrorEntity e = errors.get(i);
            if (e.getSource() == RunErrorEntity.Source.PYTHON) {
                lastPython = e;
                break;
            }
        }
        if (lastPython != null) {
            return new RunResultResponse.Error(
                    "PYTHON_EXEC_ERROR",
                    lastPython.getMessage(),
                    null
            );
        }

        String message = execution == null ? null : execution.getLastDispatchError();
        if (runRequest.getStatus() == RunLifecycleStatus.DISPATCH_FAILED) {
            return new RunResultResponse.Error("PYTHON_DISPATCH_ERROR", message, null);
        }
        return new RunResultResponse.Error("PYTHON_EXEC_ERROR", message, null);
    }

    private JsonNode parseJsonOrNull(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> parseWarningsOrNull(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeRequestId(String requestId) {
        return requestId == null ? "" : requestId.trim();
    }

    private record StoredOrRemoteResult(
            JsonNode result,
            List<String> warnings
    ) {
    }
}
