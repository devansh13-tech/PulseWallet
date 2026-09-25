-- Milestone 2: sensible default categories available to every user
-- (user_id IS NULL). Users can still add their own on top of these.
INSERT INTO categories (name, type, user_id) VALUES
    ('Salary',             'INCOME',  NULL),
    ('Freelance',          'INCOME',  NULL),
    ('Investment Returns', 'INCOME',  NULL),
    ('Other Income',       'INCOME',  NULL),
    ('Groceries',          'EXPENSE', NULL),
    ('Food & Dining',      'EXPENSE', NULL),
    ('Rent',               'EXPENSE', NULL),
    ('Utilities',          'EXPENSE', NULL),
    ('Transportation',     'EXPENSE', NULL),
    ('Healthcare',         'EXPENSE', NULL),
    ('Entertainment',      'EXPENSE', NULL),
    ('Shopping',           'EXPENSE', NULL),
    ('Education',          'EXPENSE', NULL),
    ('Other Expense',      'EXPENSE', NULL);
