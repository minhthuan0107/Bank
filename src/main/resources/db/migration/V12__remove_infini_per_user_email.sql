-- All new applications use the configured merchant email.
-- Keep historical email snapshots on cards/card_creation_orders unchanged.
-- V11 is preserved so installations that already applied it retain valid checksums.
ALTER TABLE users
    DROP COLUMN infini_member_email;
