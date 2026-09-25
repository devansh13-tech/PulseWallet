-- Financial goals are user-owned planning records. Current amount is kept
-- separately because the existing transaction ledger has no goal linkage.
CREATE TABLE financial_goals (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name           VARCHAR(120) NOT NULL,
    goal_type      VARCHAR(30) NOT NULL,
    target_amount  NUMERIC(19, 2) NOT NULL,
    current_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    target_date    DATE NOT NULL,
    description    VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_financial_goals_target_positive CHECK (target_amount > 0),
    CONSTRAINT chk_financial_goals_current_nonnegative CHECK (current_amount >= 0),
    CONSTRAINT chk_financial_goals_current_within_target CHECK (current_amount <= target_amount),
    CONSTRAINT chk_financial_goals_type CHECK (goal_type IN ('BIKE', 'CAR', 'HOUSE', 'TRIP', 'EDUCATION', 'EMERGENCY_FUND', 'CUSTOM'))
);

CREATE INDEX idx_financial_goals_user_target_date ON financial_goals (user_id, target_date);