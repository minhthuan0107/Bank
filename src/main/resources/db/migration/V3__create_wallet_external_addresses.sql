CREATE TABLE wallet_external_addresses (
                                           id BIGINT NOT NULL AUTO_INCREMENT,

                                           wallet_id BIGINT NOT NULL,

                                           network VARCHAR(20) NOT NULL,

                                           address VARCHAR(255) NOT NULL,

                                           created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                           updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                           PRIMARY KEY (id),

                                           CONSTRAINT uk_wallet_external_addresses_wallet_id
                                               UNIQUE (wallet_id),

                                           CONSTRAINT fk_wallet_external_addresses_wallet
                                               FOREIGN KEY (wallet_id)
                                                   REFERENCES wallets(id)
);