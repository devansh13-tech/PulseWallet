-- ---------------------------------------------------------------------------
-- V14: Add card network and annual fee to credit cards table
-- ---------------------------------------------------------------------------
-- Safe card metadata additions for PulseWallet M5.11 Credit Card Management.
-- NEVER stores full card number, CVV, PIN, OTP, or net-banking credentials.
-- ---------------------------------------------------------------------------

ALTER TABLE credit_cards
    ADD COLUMN IF NOT EXISTS card_network VARCHAR(50),
    ADD COLUMN IF NOT EXISTS annual_fee NUMERIC(19, 2) DEFAULT 0;
