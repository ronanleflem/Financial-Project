CREATE TABLE IF NOT EXISTS run_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    spec_type VARCHAR(64) NOT NULL,
    catalog_version VARCHAR(64) NOT NULL,
    payload_in TEXT NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    spec_generated TEXT NOT NULL,
    spec_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_run_request_request_id (request_id),
    INDEX idx_run_request_request_id (request_id),
    INDEX idx_run_request_status (status),
    INDEX idx_run_request_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS run_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    python_job_id VARCHAR(255),
    dispatch_attempts INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    last_dispatch_error TEXT,
    result_json TEXT NULL,
    warnings_json TEXT NULL,
    UNIQUE KEY uk_run_execution_request_id (request_id),
    INDEX idx_run_exec_request_id (request_id),
    INDEX idx_run_exec_python_job_id (python_job_id),
    INDEX idx_run_exec_status (status),
    CONSTRAINT fk_run_exec_request_id FOREIGN KEY (request_id) REFERENCES run_request(request_id)
);

CREATE TABLE IF NOT EXISTS run_error (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    source VARCHAR(32) NOT NULL,
    code VARCHAR(255),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_run_error_request_id (request_id),
    INDEX idx_run_error_created_at (created_at),
    CONSTRAINT fk_run_error_request_id FOREIGN KEY (request_id) REFERENCES run_request(request_id)
);

CREATE TABLE IF NOT EXISTS run_artifact (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    path VARCHAR(2000) NOT NULL,
    meta TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_run_artifact_request_id (request_id),
    INDEX idx_run_artifact_type (type),
    CONSTRAINT fk_run_artifact_request_id FOREIGN KEY (request_id) REFERENCES run_request(request_id)
);
