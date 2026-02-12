package finance.project.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.repositories.CanonicalRunAuditRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanonicalRunAuditServiceBestEffortTest {

    @Test
    void doesNotFailProxyWhenRepositoryIsUnavailable() {
        CanonicalRunAuditRepository repository = mock(CanonicalRunAuditRepository.class);
        when(repository.findByRequestId("run_1")).thenThrow(new RuntimeException("db down"));

        CanonicalRunAuditService service = new CanonicalRunAuditService(repository, new ObjectMapper());
        var response = org.springframework.http.ResponseEntity.ok(java.util.Map.of("requestId", "run_1", "status", "PENDING"));

        assertDoesNotThrow(() -> service.recordSubmit("{\"specType\":\"backtest\"}", "corr-1", "alice", response));
        assertDoesNotThrow(() -> service.recordLifecycle("run_1", "corr-1", "alice", response));
    }
}
