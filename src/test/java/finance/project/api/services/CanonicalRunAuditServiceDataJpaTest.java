package finance.project.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.run.CanonicalRunAuditEntity;
import finance.project.api.repositories.CanonicalRunAuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(CanonicalRunAuditServiceDataJpaTest.Config.class)
class CanonicalRunAuditServiceDataJpaTest {

    @TestConfiguration
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        CanonicalRunAuditService canonicalRunAuditService(CanonicalRunAuditRepository repository, ObjectMapper objectMapper) {
            return new CanonicalRunAuditService(repository, objectMapper);
        }
    }

    @Autowired
    private CanonicalRunAuditService service;

    @Autowired
    private CanonicalRunAuditRepository repository;

    @Test
    void createsAndUpdatesCanonicalAuditWithoutDuplicates() {
        String payload = """
                {"specType":"backtest","requestId":"req-legacy-id"}
                """;
        ResponseEntity<?> submitResponse = ResponseEntity.ok(java.util.Map.of(
                "requestId", "run_1",
                "status", "PENDING"
        ));

        service.recordSubmit(payload, "corr-1", "alice", submitResponse);

        CanonicalRunAuditEntity created = repository.findByRequestId("run_1").orElseThrow();
        assertEquals("alice", created.getActor());
        assertEquals("backtest", created.getSpecType());
        assertEquals("PENDING", created.getStatus());
        assertEquals("corr-1", created.getCorrelationId());

        ResponseEntity<?> statusResponse = ResponseEntity.ok(java.util.Map.of(
                "requestId", "run_1",
                "status", "RUNNING"
        ));
        service.recordLifecycle("run_1", "corr-2", "alice", statusResponse);

        CanonicalRunAuditEntity updated = repository.findByRequestId("run_1").orElseThrow();
        assertEquals("RUNNING", updated.getStatus());
        assertEquals("corr-2", updated.getCorrelationId());
        assertEquals(1, repository.count());
    }

    @Test
    void supportsSnakeCaseSpecTypeAndRequestIdFields() {
        String payload = """
                {"spec_type":"dca","request_id":"req-snake-id"}
                """;
        ResponseEntity<?> submitResponse = ResponseEntity.ok(java.util.Map.of(
                "request_id", "run_snake_1",
                "status", "PENDING"
        ));

        service.recordSubmit(payload, "corr-snake", "bob", submitResponse);

        CanonicalRunAuditEntity created = repository.findByRequestId("run_snake_1").orElseThrow();
        assertEquals("bob", created.getActor());
        assertEquals("dca", created.getSpecType());
        assertEquals("PENDING", created.getStatus());
        assertEquals("corr-snake", created.getCorrelationId());
    }

    @Test
    void usesPathRequestIdWhenBodyHasNoRequestId() {
        ResponseEntity<?> response = ResponseEntity.ok(java.util.Map.of("status", "CANCELLING"));
        service.recordLifecycle("run_from_path", "corr-path", null, response);

        assertTrue(repository.findByRequestId("run_from_path").isPresent());
    }
}
