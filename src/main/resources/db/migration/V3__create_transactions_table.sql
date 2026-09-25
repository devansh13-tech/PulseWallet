-- Milestone 2: the core transaction ledger.
-- category_id uses ON DELETE SET NULL rather than CASCADE: deleting a
-- category should not destroy a user's transaction history.
CREATE TABLE transactions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id      BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    amount           NUMERIC(19, 2) NOT NULL,
    description      VARCHAR(255),
    type             VARCHAR(20) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_type CHECK (type IN ('INCOME', 'EXPENSE'))
);

-- Every dashboard/history query filters by user first, then narrows by date
-- or category, so both composite indexes lead with user_id.
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date);
CREATE INDEX idx_transactions_user_category ON transactions (user_id, category_id);
