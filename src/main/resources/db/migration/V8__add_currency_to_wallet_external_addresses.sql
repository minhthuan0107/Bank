-- 1. Thêm currency (cho phép NULL tạm thời để xử lý dữ liệu cũ).
ALTER TABLE wallet_external_addresses
    ADD COLUMN currency VARCHAR(10) NULL AFTER wallet_id;

-- 2. Xác định currency dựa trên network của bản ghi cũ.
UPDATE wallet_external_addresses
SET currency = CASE
                   WHEN network IN ('TRC20', 'BEP20') THEN 'USDT'
                   WHEN network = 'BASE' THEN 'USDC'
                   ELSE NULL
    END
WHERE currency IS NULL;

-- 3. Bắt buộc mỗi external address phải có currency.
ALTER TABLE wallet_external_addresses
    MODIFY COLUMN currency VARCHAR(10) NOT NULL;

-- 4. Chỉ cho phép các tổ hợp currency + network hợp lệ.
ALTER TABLE wallet_external_addresses
    ADD CONSTRAINT chk_wallet_external_addresses_currency_network
        CHECK (
            (currency = 'USDT' AND network IN ('TRC20', 'BEP20'))
                OR
            (currency = 'USDC' AND network = 'BASE')
            );