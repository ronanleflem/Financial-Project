ALTER TABLE trades_completed
    ADD COLUMN broker VARCHAR(255),
    ADD COLUMN exchange VARCHAR(255),
    ADD COLUMN currency VARCHAR(255),
    ADD COLUMN market_type VARCHAR(255);
