-- Additive migration: preserve Slash cards/BINs/holders and their existing IDs.
-- Fixed customer pricing: card amount >= 10 USD; creation fee = 1 USD.
ALTER TABLE users
    ADD COLUMN infini_member_email VARCHAR(255) NULL;
-- Populate ONLY after confirming this member belongs to the Infini merchant.
-- Do not automatically copy every user's email into this mapping.

ALTER TABLE cards
    MODIFY COLUMN slash_card_id VARCHAR(255) NULL,
    ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'SLASH',
    ADD COLUMN provider_card_id VARCHAR(100) NULL,
    ADD COLUMN product_id INT NULL,
    ADD COLUMN token_type VARCHAR(10) NULL,
    ADD COLUMN creation_fee DECIMAL(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN holder_name VARCHAR(100) NULL,
    ADD COLUMN holder_email VARCHAR(255) NULL,
    ADD CONSTRAINT uk_cards_provider_id UNIQUE (provider, provider_card_id);

CREATE TABLE card_creation_orders (
    request_id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    product_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    holder_name VARCHAR(100) NOT NULL,
    member_email VARCHAR(255) NOT NULL,
    note VARCHAR(255) NULL,
    token_type VARCHAR(10) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    fee DECIMAL(19,4) NOT NULL,
    total_debit DECIMAL(19,4) NOT NULL,
    provider_fee DECIMAL(19,4) NULL,
    provider_total_pay DECIMAL(19,4) NULL,
    status VARCHAR(20) NOT NULL,
    provider_card_id VARCHAR(100) NULL,
    card_id BIGINT NULL,
    review_reason VARCHAR(100) NULL,
    next_check_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_creation_provider_card UNIQUE (provider_card_id),
    CONSTRAINT chk_creation_price CHECK (amount >= 10 AND fee = 1 AND total_debit = amount + fee),
    CONSTRAINT chk_creation_status CHECK (status IN ('PENDING','REVIEW_REQUIRED','SUCCEEDED','FAILED')),
    INDEX idx_creation_user_status (user_id, status),
    INDEX idx_creation_poll (status, next_check_at)
);
-- No wallet ALTER is needed: available/frozen/allocated/total already exist.
-- No changes to legacy card_bins/card_holders are needed for Infini creation.
