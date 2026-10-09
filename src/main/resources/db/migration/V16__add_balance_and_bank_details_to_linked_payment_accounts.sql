-- ---------------------------------------------------------------------------
-- V16: Add balance, account subtype and IFSC to linked_payment_accounts
-- ---------------------------------------------------------------------------
-- Enhances linked_payment_accounts table for Bank Account tracking & Dashboard visibility.
-- NEVER stores net-banking passwords, UPI PINs, OTPs, CVVs, or full credentials.
-- ---------------------------------------------------------------------------

ALTER TABLE linked_payment_accounts
    ADD COLUMN IF NOT EXISTS balance NUMERIC(19, 2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS account_subtype VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ifsc_code VARCHAR(20);
