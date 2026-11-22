-- Records the moment right before the Infini apply request is sent.
-- NULL means the request was never sent, so the held funds can be released safely.
ALTER TABLE card_creation_orders
    ADD COLUMN apply_started_at DATETIME(6) NULL;

CREATE INDEX idx_creation_unsubmitted
    ON card_creation_orders (status, created_at);
