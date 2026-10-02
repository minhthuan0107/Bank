ALTER TABLE deposit_orders
    ADD COLUMN source_address VARCHAR(255) NULL
        AFTER address;