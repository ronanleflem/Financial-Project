package finance.project.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.run.CanonicalRunAuditEntity;
import finance.project.api.repositories.CanonicalRunAuditRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CanonicalRunAuditService {
    private static final Logger log = LoggerFactory.getLogger(CanonicalRunAuditService.class);

    private final CanonicalRunAuditRepository repository;
    private final ObjectMapper objectMapper;

    public CanonicalRunAuditService(CanonicalRunAuditRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void recordSubmit(String rawPayload, String correlationId, String actor, ResponseEntity<?> response) {
        String requestId = firstNonBlank(
                extractFirstText(response.getBody(), "request_id", "requestId", "run_id"),
                extractFirstText(rawPayload, "request_id", "requestId")
        );
        String specType = extractFirstText(rawPayload, "spec_type", "specType");
        String status = extractText(response.getBody(), "status");
        upsertBestEffort(requestId, actor, specType, status, correlationId);
    }

    public void recordLifecycle(String pathRequestId, String correlationId, String actor, ResponseEntity<?> response) {
        String requestId = firstNonBlank(
                extractFirstText(response.getBody(), "request_id", "requestId", "run_id"),
                normalize(pathRequestId)
        );
        String status = extractText(response.getBody(), "status");
        upsertBestEffort(requestId, actor, null, status, correlationId);
    }

    private void upsertBestEffort(String requestId,
                                  String actor,
                                  String specType,
                                  String status,
                                  String correlationId) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }
        try {
            Optional<CanonicalRunAuditEntity> optional = repository.findByRequestId(requestId);
            CanonicalRunAuditEntity entity = optional.orElseGet(() -> CanonicalRunAuditEntity.builder()
                    .requestId(requestId)
                    .build());
            if (actor != null && !actor.isBlank()) {
                entity.setActor(actor);
            }
            if (specType != null && !specType.isBlank()) {
                entity.setSpecType(specType);
            }
            if (status != null && !status.isBlank()) {
                entity.setStatus(status);
            }
            if (correlationId != null && !correlationId.isBlank()) {
                entity.setCorrelationId(correlationId);
            }
            repository.save(entity);
        } catch (Exception ex) {
            log.warn("canonical_run_audit_write_failed requestId={} message={}", requestId, safeMessage(ex));
        }
    }

    private String extractText(String rawPayload, String fieldName) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            return textOrNull(root.get(fieldName));
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractFirstText(String rawPayload, String... fieldNames) {
        if (fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            String value = extractText(rawPayload, fieldName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String extractText(Object body, String fieldName) {
        if (body == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.valueToTree(body);
            return textOrNull(root.get(fieldName));
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractFirstText(Object body, String... fieldNames) {
        if (fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            String value = extractText(body, fieldName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return normalize(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
