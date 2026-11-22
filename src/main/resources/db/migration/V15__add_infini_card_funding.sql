-- Top-up / redeem orders for Infini cards. One row per client request ID.
CREATE TABLE infini_funding_orders (
    request_id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    infini_card_id BIGINT NOT NULL,
    provider_card_id VARCHAR(100) NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    token_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    review_reason VARCHAR(100) NULL,
    provider_tx_id VARCHAR(100) NULL,
    provider_card_balance DECIMAL(19,4) NULL,
    submit_started_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_funding_provider_tx UNIQUE (provider_tx_id),
    CONSTRAINT fk_funding_infini_card FOREIGN KEY (infini_card_id) REFERENCES cards_infini(id),
    CONSTRAINT chk_funding_type CHECK (type IN ('TOP_UP','REDEEM')),
    CONSTRAINT chk_funding_status CHECK (status IN ('PENDING','REVIEW_REQUIRED','SUCCEEDED','FAILED')),
    CONSTRAINT chk_funding_amount CHECK (amount >= 10),
    INDEX idx_funding_card_status (infini_card_id, status),
    INDEX idx_funding_status_created (status, created_at),
    INDEX idx_funding_user (user_id, created_at)
);

-- Signed Infini webhook events, stored before processing. event_id deduplicates retries.
CREATE TABLE infini_webhook_events (
    event_id VARCHAR(100) NOT NULL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    payload MEDIUMTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(100) NULL,
    received_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    INDEX idx_infini_webhook_status (status, received_at)
);
