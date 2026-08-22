-- Milestone 2: expense/income categories.
-- user_id NULL = system default category, visible to every user.
-- user_id set  = a category a specific user created for themselves.
CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(60) NOT NULL,
    type       VARCHAR(20) NOT NULL,
    user_id    BIGINT REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_categories_type CHECK (type IN ('INCOME', 'EXPENSE')),
    -- NULLs are treated as distinct by Postgres, so this only stops one user
    -- from creating two categories with the same name - it never collides
    -- with the system defaults.
    CONSTRAINT uk_categories_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_categories_user ON categories (user_id);
