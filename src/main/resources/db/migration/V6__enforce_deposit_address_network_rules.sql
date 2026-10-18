ALTER TABLE deposit_addresses
    ADD CONSTRAINT uk_deposit_addresses_network
        UNIQUE (network);

ALTER TABLE deposit_addresses
    ADD CONSTRAINT chk_deposit_addresses_currency_network
        CHECK (
            (currency = 'USDT' AND network IN ('TRC20', 'BEP20'))
                OR
            (currency = 'USDC' AND network = 'BASE')
            );