-- Milestone 2: basic budget records (limit + period).
-- The Milestone 3 budget ENGINE (salary - expenses, forecasting, advisory)
-- is not implemented here - this is just the data model and CRUD it reads.
CREATE TABLE budgets (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_budgets_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_budgets_date_range CHECK (end_date >= start_date)
);

CREATE INDEX idx_budgets_user_period ON budgets (user_id, start_date, end_date);
