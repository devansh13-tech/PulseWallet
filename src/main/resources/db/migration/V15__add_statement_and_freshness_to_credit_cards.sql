-- ---------------------------------------------------------------------------
-- V15: Add statement balance, minimum due, data source and sync timestamp
-- ---------------------------------------------------------------------------
-- Enhances credit_cards table for M5.11 Smart Credit Card Intelligence.
-- NEVER stores full card number, CVV, PIN, OTP, or net-banking credentials.
-- ---------------------------------------------------------------------------

ALTER TABLE credit_cards
    ADD COLUMN IF NOT EXISTS statement_balance NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS minimum_due NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(50) DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS last_statement_sync_at TIMESTAMP WITH TIME ZONE;
