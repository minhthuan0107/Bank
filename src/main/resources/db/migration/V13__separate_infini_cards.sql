-- Preserve V11/V12 checksums. New Infini cards are stored only in this table.
CREATE TABLE cards_infini (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    infini_card_id VARCHAR(100) NOT NULL,
    product_id INT NOT NULL,
    token_type VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    holder_name VARCHAR(100) NOT NULL,
    holder_email VARCHAR(255) NOT NULL,
    note VARCHAR(255) NULL,
    currency VARCHAR(10) NOT NULL,
    initial_amount DECIMAL(19,4) NOT NULL,
    remaining_amount DECIMAL(19,4) NOT NULL,
    spent_amount DECIMAL(19,4) NOT NULL,
    locked_amount DECIMAL(19,4) NOT NULL,
    creation_fee DECIMAL(19,4) NOT NULL,
    last4 VARCHAR(4) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_cards_infini_external_id UNIQUE (infini_card_id),
    INDEX idx_cards_infini_user (user_id, id)
);

-- Copy any Infini cards already issued by the transitional implementation.
-- Keep their local IDs so creation orders retain their existing card reference.
-- Old rows in cards are preserved for audit, but excluded from legacy ORM reads.
INSERT INTO cards_infini (
    id, user_id, infini_card_id, product_id, token_type, name,
    holder_name, holder_email, note, currency, initial_amount,
    remaining_amount, spent_amount, locked_amount, creation_fee,
    last4, status, created_at, updated_at
)
SELECT
    id, user_id, provider_card_id, product_id, token_type, name,
    holder_name, holder_email, note, currency, card_limit,
    remaining_amount, spent_amount, COALESCE(locked_amount, 0), creation_fee,
    last4, status, created_at, updated_at
FROM cards
WHERE provider = 'INFINI';

ALTER TABLE card_creation_orders
    CHANGE COLUMN card_id infini_card_id BIGINT NULL,
    ADD CONSTRAINT fk_creation_infini_card
        FOREIGN KEY (infini_card_id) REFERENCES cards_infini(id);

-- No wallet balance updates: this migration only separates stored card data.
-- IDs belong to separate namespaces. Never send an Infini local ID to Slash APIs.

-- Place future Infini IDs above existing legacy IDs to reduce accidental overlap
-- during the transition. API namespaces must still remain separate.
SET @infini_next_card_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM cards);
SET @infini_auto_increment_sql = CONCAT(
    'ALTER TABLE cards_infini AUTO_INCREMENT = ', @infini_next_card_id
);
PREPARE infini_auto_increment_stmt FROM @infini_auto_increment_sql;
EXECUTE infini_auto_increment_stmt;
DEALLOCATE PREPARE infini_auto_increment_stmt;
