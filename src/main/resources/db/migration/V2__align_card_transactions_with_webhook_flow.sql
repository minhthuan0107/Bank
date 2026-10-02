-- =========================================================
-- Đồng bộ card_transactions với entity CardTransaction
-- và flow webhook Slash mới
-- =========================================================


-- 1. slash_transaction_id là ID duy nhất của transaction bên Slash
ALTER TABLE card_transactions
    MODIFY COLUMN slash_transaction_id VARCHAR(100) NOT NULL;


-- 2. slash_event_id chỉ giữ event gần nhất đã sync
-- Không còn dùng để deduplicate
ALTER TABLE card_transactions
    MODIFY COLUMN slash_event_id VARCHAR(100) NULL;


-- 3. ID card của Slash
ALTER TABLE card_transactions
    MODIFY COLUMN card_id VARCHAR(100) NOT NULL;


-- 4. ID user nội bộ Bank
ALTER TABLE card_transactions
    MODIFY COLUMN user_id BIGINT NOT NULL;


-- 5. Bank lưu amount dưới dạng giá trị tuyệt đối, số dương
ALTER TABLE card_transactions
    MODIFY COLUMN amount DECIMAL(19,4) NOT NULL;


-- 6. Currency mặc định USD
ALTER TABLE card_transactions
    MODIFY COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'USD';


-- 7. Trạng thái local:
-- PENDING / POSTED / FAILED / REVERSED
ALTER TABLE card_transactions
    MODIFY COLUMN status VARCHAR(20) NOT NULL;


-- 8. detailed_status giữ nguyên trạng thái chi tiết từ Slash
ALTER TABLE card_transactions
    MODIFY COLUMN detailed_status VARCHAR(100) NULL;


-- 9. Các metadata giao dịch
ALTER TABLE card_transactions
    MODIFY COLUMN merchant_description VARCHAR(255) NULL,
    MODIFY COLUMN description VARCHAR(255) NULL,
    MODIFY COLUMN decline_reason VARCHAR(255) NULL;


-- 10. Payload webhook gần nhất
ALTER TABLE card_transactions
    MODIFY COLUMN raw_payload TEXT NULL;


-- 11. Một Slash transaction chỉ được có một record local
ALTER TABLE card_transactions
    ADD CONSTRAINT uk_card_transactions_slash_transaction_id
        UNIQUE (slash_transaction_id);


-- 12. Index phục vụ query Admin / history / statistics
CREATE INDEX idx_card_transactions_user_id
    ON card_transactions (user_id);

CREATE INDEX idx_card_transactions_card_id
    ON card_transactions (card_id);

CREATE INDEX idx_card_transactions_user_status
    ON card_transactions (user_id, status);

CREATE INDEX idx_card_transactions_transaction_date
    ON card_transactions (transaction_date);

