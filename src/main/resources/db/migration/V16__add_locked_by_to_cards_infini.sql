-- Who locked the card: USER (owner), ACCOUNT (admin locked the account), ADMIN (admin locked this card).
ALTER TABLE cards_infini
    ADD COLUMN locked_by VARCHAR(10) NULL;

-- The origin of existing locks is unknown: only an admin may unlock them.
UPDATE cards_infini SET locked_by = 'ADMIN' WHERE status = 'BLOCKED';
