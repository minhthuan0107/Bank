-- The minimum is enforced by the application (InfiniFundingPolicy), so it can change without a migration.
ALTER TABLE infini_funding_orders DROP CHECK chk_funding_amount;
ALTER TABLE infini_funding_orders ADD CONSTRAINT chk_funding_amount CHECK (amount > 0);
