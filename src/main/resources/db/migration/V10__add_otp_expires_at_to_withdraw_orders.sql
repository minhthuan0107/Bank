-- Add OTP expiration timestamp
ALTER TABLE withdraw_orders
    ADD COLUMN otp_expires_at DATETIME(6) NULL;

-- Optimize queries for expired pending OTP orders
CREATE INDEX idx_withdraw_orders_otp_expiry
    ON withdraw_orders (status, otp_expires_at);