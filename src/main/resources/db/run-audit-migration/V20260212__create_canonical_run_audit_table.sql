CREATE TABLE IF NOT EXISTS canonical_run_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    actor VARCHAR(255) NULL,
    spec_type VARCHAR(64) NULL,
    status VARCHAR(64) NULL,
    correlation_id VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_canonical_run_audit_request_id (request_id),
    INDEX idx_canonical_run_audit_status (status),
    INDEX idx_canonical_run_audit_created_at (created_at)
);
