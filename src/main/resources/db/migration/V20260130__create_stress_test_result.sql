CREATE TABLE IF NOT EXISTS stress_test_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_id VARCHAR(255),
    run_id VARCHAR(255) NOT NULL,
    asset_class VARCHAR(255),
    symbol VARCHAR(255),
    timeframe VARCHAR(255),
    mode VARCHAR(32) NOT NULL,
    payload_json JSON NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stress_test_result_run_id (run_id),
    INDEX idx_stress_test_result_strategy_id (strategy_id),
    INDEX idx_stress_test_result_mode (mode)
);
