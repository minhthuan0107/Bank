RENAME TABLE wallet_currency_settings TO wallet_settings;

ALTER TABLE wallet_settings
DROP COLUMN currency;

ALTER TABLE wallet_settings
    MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE wallet_settings
    ADD CONSTRAINT chk_wallet_settings_singleton
        CHECK (id = 1);