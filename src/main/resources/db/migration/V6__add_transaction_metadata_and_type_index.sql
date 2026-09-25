-- Milestone 3: optional source metadata for imported and manually entered ledger rows.
ALTER TABLE transactions
    ADD COLUMN merchant VARCHAR(160),
    ADD COLUMN payment_channel VARCHAR(40);

CREATE INDEX idx_transactions_user_type_date
    ON transactions (user_id, type, transaction_date);