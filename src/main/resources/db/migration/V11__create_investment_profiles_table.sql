CREATE TABLE investment_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    risk_preference     VARCHAR(20) NOT NULL,
    time_horizon_years  INTEGER NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_investment_profiles_risk CHECK (risk_preference IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    CONSTRAINT chk_investment_profiles_horizon CHECK (time_horizon_years > 0)
);
