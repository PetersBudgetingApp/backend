-- ============================================================
-- V12: Seed example/demo user with 12 months of financial data
-- ============================================================
-- User: example.user@test.com / example-user-password1
-- Date range: March 2025 – February 2026
-- ============================================================

-- 1. CREATE DEMO USER
INSERT INTO users (email, password_hash, created_at, updated_at)
SELECT 'example.user@test.com',
       '$2b$10$TrbaIyY3qxCEupHz5q0ByOQBTN8kBLXrZXnuuRoWhZ4YJzlyD.1Re',
       TIMESTAMP '2025-02-15 10:00:00',
       TIMESTAMP '2025-02-15 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'example.user@test.com');

-- 2. CREATE ACCOUNTS
INSERT INTO accounts (user_id, name, institution_name, account_type, net_worth_category_override, currency, current_balance, available_balance, is_active, created_at, updated_at)
SELECT id, 'Primary Checking', 'Chase Bank', 'CHECKING', 'BANK_ACCOUNT', 'USD', 4523.67, 4523.67, TRUE, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users WHERE email = 'example.user@test.com';

INSERT INTO accounts (user_id, name, institution_name, account_type, net_worth_category_override, currency, current_balance, available_balance, is_active, created_at, updated_at)
SELECT id, 'Emergency Savings', 'Chase Bank', 'SAVINGS', 'BANK_ACCOUNT', 'USD', 15250.00, 15250.00, TRUE, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users WHERE email = 'example.user@test.com';

INSERT INTO accounts (user_id, name, institution_name, account_type, net_worth_category_override, currency, current_balance, available_balance, is_active, created_at, updated_at)
SELECT id, 'Visa Rewards Card', 'Capital One', 'CREDIT_CARD', 'LIABILITY', 'USD', -2147.83, -2147.83, TRUE, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users WHERE email = 'example.user@test.com';

INSERT INTO accounts (user_id, name, institution_name, account_type, net_worth_category_override, currency, current_balance, available_balance, is_active, created_at, updated_at)
SELECT id, 'Auto Loan', 'Capital One', 'LOAN', 'LIABILITY', 'USD', -12380.00, -12380.00, TRUE, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users WHERE email = 'example.user@test.com';

INSERT INTO accounts (user_id, name, institution_name, account_type, net_worth_category_override, currency, current_balance, available_balance, is_active, created_at, updated_at)
SELECT id, 'Vanguard Brokerage', 'Vanguard', 'INVESTMENT', 'INVESTMENT', 'USD', 47832.50, 47832.50, TRUE, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users WHERE email = 'example.user@test.com';

INSERT INTO accounts (user_id, name, institution_name, account_type, net_worth_category_override, currency, current_balance, available_balance, is_active, created_at, updated_at)
SELECT id, 'Vacation Fund', 'Ally Bank', 'SAVINGS', 'BANK_ACCOUNT', 'USD', 3200.00, 3200.00, TRUE, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users WHERE email = 'example.user@test.com';

-- 3. CUSTOM CATEGORIES
INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order, created_at, updated_at)
SELECT u.id, c.id, 'Side Hustle', 'laptop', '#22C55E', 'INCOME', FALSE, 6, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Income' AND c.user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order, created_at, updated_at)
SELECT u.id, c.id, 'Loan Payments', 'landmark', '#64748B', 'EXPENSE', FALSE, 5, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Bills & Fees' AND c.user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order, created_at, updated_at)
SELECT u.id, c.id, 'Date Night', 'heart', '#EF4444', 'EXPENSE', FALSE, 7, TIMESTAMP '2025-02-15 10:00:00', TIMESTAMP '2025-02-15 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Entertainment' AND c.user_id IS NULL;

-- 4. USER CATEGORIZATION RULES
INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_active, is_system, condition_operator, created_at, updated_at)
SELECT u.id, 'Trader Joes', 'TRADER JOE', 'CONTAINS', 'DESCRIPTION', c.id, 100, TRUE, FALSE, 'AND', TIMESTAMP '2025-03-10 10:00:00', TIMESTAMP '2025-03-10 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Groceries' AND c.user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_active, is_system, condition_operator, created_at, updated_at)
SELECT u.id, 'Olive Garden', 'OLIVE GARDEN', 'CONTAINS', 'DESCRIPTION', c.id, 100, TRUE, FALSE, 'AND', TIMESTAMP '2025-03-10 10:00:00', TIMESTAMP '2025-03-10 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Restaurants' AND c.user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_active, is_system, condition_operator, created_at, updated_at)
SELECT u.id, 'Petco', 'PETCO', 'CONTAINS', 'DESCRIPTION', c.id, 100, TRUE, FALSE, 'AND', TIMESTAMP '2025-03-10 10:00:00', TIMESTAMP '2025-03-10 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Pet Food' AND c.user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_active, is_system, condition_operator, created_at, updated_at)
SELECT u.id, 'State Farm', 'STATE FARM', 'CONTAINS', 'DESCRIPTION', c.id, 100, TRUE, FALSE, 'AND', TIMESTAMP '2025-03-10 10:00:00', TIMESTAMP '2025-03-10 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Auto Insurance' AND c.user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_active, is_system, condition_operator, created_at, updated_at)
SELECT u.id, 'Blue Cross', 'BLUE CROSS', 'CONTAINS', 'DESCRIPTION', c.id, 100, TRUE, FALSE, 'AND', TIMESTAMP '2025-03-10 10:00:00', TIMESTAMP '2025-03-10 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Health Insurance' AND c.user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_active, is_system, condition_operator, created_at, updated_at)
SELECT u.id, 'Oakwood Rent', 'OAKWOOD', 'CONTAINS', 'DESCRIPTION', c.id, 100, TRUE, FALSE, 'AND', TIMESTAMP '2025-03-10 10:00:00', TIMESTAMP '2025-03-10 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Rent/Mortgage' AND c.user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_active, is_system, condition_operator, created_at, updated_at)
SELECT u.id, 'Capital One Auto', 'CAPITAL ONE AUTO', 'CONTAINS', 'DESCRIPTION', c.id, 100, TRUE, FALSE, 'AND', TIMESTAMP '2025-03-10 10:00:00', TIMESTAMP '2025-03-10 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Loan Payments' AND c.user_id = u.id;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_active, is_system, condition_operator, created_at, updated_at)
SELECT u.id, 'City Electric', 'CITY ELECTRIC', 'CONTAINS', 'DESCRIPTION', c.id, 100, TRUE, FALSE, 'AND', TIMESTAMP '2025-03-10 10:00:00', TIMESTAMP '2025-03-10 10:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Utilities' AND c.user_id IS NULL;

-- ============================================================
-- 5. TRANSACTIONS
-- ============================================================

-- ----- CHECKING: Salary (24 entries, bi-monthly) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, 3200.00, 'PAYROLL - ACME CORP DIRECT DEPOSIT', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-01 07:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-03-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-04-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-04-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-15 07:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-01 07:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-15 07:00:00'
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Salary' AND c.user_id IS NULL;

-- ----- CHECKING: Freelance income (5 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-04-22 14:00:00' d, 750.00 amt, 'FREELANCE - WEB DESIGN PROJECT' descr UNION ALL
    SELECT TIMESTAMP '2025-07-11 14:00:00', 1200.00, 'FREELANCE - LOGO DESIGN CLIENT' UNION ALL
    SELECT TIMESTAMP '2025-09-18 14:00:00', 500.00, 'FREELANCE - CONSULTING SESSION' UNION ALL
    SELECT TIMESTAMP '2025-11-05 14:00:00', 900.00, 'FREELANCE - WEBSITE REDESIGN' UNION ALL
    SELECT TIMESTAMP '2026-01-28 14:00:00', 650.00, 'FREELANCE - BRANDING PACKAGE'
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Side Hustle' AND c.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com');

-- ----- CHECKING: Rent (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -1650.00, 'RENT PAYMENT - OAKWOOD APT 4B', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-01 09:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-01 09:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-01 09:00:00'
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Rent/Mortgage' AND c.user_id IS NULL;

-- ----- CHECKING: Health Insurance (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -320.00, 'BLUE CROSS HEALTH PREMIUM', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-01 08:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-01 08:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-01 08:00:00'
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Health Insurance' AND c.user_id IS NULL;

-- ----- CHECKING: Utilities (12 entries, seasonal variation) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, 'CITY ELECTRIC UTILITY CO', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-05 10:00:00' d, -128.45 amt UNION ALL
    SELECT TIMESTAMP '2025-04-05 10:00:00', -122.30 UNION ALL
    SELECT TIMESTAMP '2025-05-05 10:00:00', -135.67 UNION ALL
    SELECT TIMESTAMP '2025-06-05 10:00:00', -168.90 UNION ALL
    SELECT TIMESTAMP '2025-07-05 10:00:00', -189.23 UNION ALL
    SELECT TIMESTAMP '2025-08-05 10:00:00', -192.10 UNION ALL
    SELECT TIMESTAMP '2025-09-05 10:00:00', -165.45 UNION ALL
    SELECT TIMESTAMP '2025-10-05 10:00:00', -138.20 UNION ALL
    SELECT TIMESTAMP '2025-11-05 10:00:00', -142.80 UNION ALL
    SELECT TIMESTAMP '2025-12-05 10:00:00', -158.35 UNION ALL
    SELECT TIMESTAMP '2026-01-05 10:00:00', -172.60 UNION ALL
    SELECT TIMESTAMP '2026-02-05 10:00:00', -165.15
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Utilities' AND c.user_id IS NULL;

-- ----- CHECKING: Car Insurance (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -145.00, 'STATE FARM AUTO INS PREMIUM', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-10 09:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-10 09:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-10 09:00:00'
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Auto Insurance' AND c.user_id IS NULL;

-- ----- CHECKING: Car Loan (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -385.00, 'CAPITAL ONE AUTO FINANCE PMT', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-15 09:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-15 09:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-15 09:00:00'
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Loan Payments' AND c.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com');

-- ----- CHECKING: Transfer to Savings (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, is_internal_transfer, exclude_from_totals, created_at, updated_at)
SELECT a.id, v.d, -500.00, 'TRANSFER TO EMERGENCY SAVINGS', c.id, FALSE, TRUE, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-20 10:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-20 10:00:00'
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Transfers' AND c.user_id IS NULL;

-- ----- SAVINGS: Transfer from Checking (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, is_internal_transfer, exclude_from_totals, created_at, updated_at)
SELECT a.id, v.d, 500.00, 'TRANSFER FROM PRIMARY CHECKING', c.id, FALSE, TRUE, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-20 10:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-20 10:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-20 10:00:00'
) v
JOIN accounts a ON a.name = 'Emergency Savings' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Transfers' AND c.user_id IS NULL;

-- ----- CHECKING: Credit Card Payment (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, is_internal_transfer, exclude_from_totals, created_at, updated_at)
SELECT a.id, v.d, v.amt, 'VISA REWARDS CARD PAYMENT', c.id, FALSE, TRUE, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-25 10:00:00' d, -2100.00 amt UNION ALL
    SELECT TIMESTAMP '2025-04-25 10:00:00', -2250.00 UNION ALL
    SELECT TIMESTAMP '2025-05-25 10:00:00', -1980.00 UNION ALL
    SELECT TIMESTAMP '2025-06-25 10:00:00', -2320.00 UNION ALL
    SELECT TIMESTAMP '2025-07-25 10:00:00', -2150.00 UNION ALL
    SELECT TIMESTAMP '2025-08-25 10:00:00', -2400.00 UNION ALL
    SELECT TIMESTAMP '2025-09-25 10:00:00', -2080.00 UNION ALL
    SELECT TIMESTAMP '2025-10-25 10:00:00', -2190.00 UNION ALL
    SELECT TIMESTAMP '2025-11-25 10:00:00', -2550.00 UNION ALL
    SELECT TIMESTAMP '2025-12-25 10:00:00', -2700.00 UNION ALL
    SELECT TIMESTAMP '2026-01-25 10:00:00', -2050.00 UNION ALL
    SELECT TIMESTAMP '2026-02-25 10:00:00', -2200.00
) v
JOIN accounts a ON a.name = 'Primary Checking' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Transfers' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Payment Received (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, is_internal_transfer, exclude_from_totals, created_at, updated_at)
SELECT a.id, v.d, v.amt, 'PAYMENT RECEIVED - THANK YOU', c.id, FALSE, TRUE, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-25 10:00:00' d, 2100.00 amt UNION ALL
    SELECT TIMESTAMP '2025-04-25 10:00:00', 2250.00 UNION ALL
    SELECT TIMESTAMP '2025-05-25 10:00:00', 1980.00 UNION ALL
    SELECT TIMESTAMP '2025-06-25 10:00:00', 2320.00 UNION ALL
    SELECT TIMESTAMP '2025-07-25 10:00:00', 2150.00 UNION ALL
    SELECT TIMESTAMP '2025-08-25 10:00:00', 2400.00 UNION ALL
    SELECT TIMESTAMP '2025-09-25 10:00:00', 2080.00 UNION ALL
    SELECT TIMESTAMP '2025-10-25 10:00:00', 2190.00 UNION ALL
    SELECT TIMESTAMP '2025-11-25 10:00:00', 2550.00 UNION ALL
    SELECT TIMESTAMP '2025-12-25 10:00:00', 2700.00 UNION ALL
    SELECT TIMESTAMP '2026-01-25 10:00:00', 2050.00 UNION ALL
    SELECT TIMESTAMP '2026-02-25 10:00:00', 2200.00
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Transfers' AND c.user_id IS NULL;

-- ----- BROKERAGE: Quarterly Dividends -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, 'VANGUARD DIVIDEND REINVEST', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-28 09:00:00' d, 312.50 amt UNION ALL
    SELECT TIMESTAMP '2025-06-27 09:00:00', 325.80 UNION ALL
    SELECT TIMESTAMP '2025-09-26 09:00:00', 318.90 UNION ALL
    SELECT TIMESTAMP '2025-12-29 09:00:00', 340.25
) v
JOIN accounts a ON a.name = 'Vanguard Brokerage' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Investments' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Groceries (36 entries, 3/month) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-04 15:30:00' d, -87.23 amt, 'WHOLE FOODS MARKET #1042' descr UNION ALL
    SELECT TIMESTAMP '2025-03-12 16:00:00', -52.14, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2025-03-22 14:45:00', -64.89, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2025-04-03 15:15:00', -92.45, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-04-11 16:30:00', -48.67, 'TARGET #1234' UNION ALL
    SELECT TIMESTAMP '2025-04-21 14:00:00', -71.23, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2025-05-02 15:45:00', -78.90, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2025-05-10 16:15:00', -95.34, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-05-20 14:30:00', -55.12, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2025-06-03 15:00:00', -83.67, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-06-13 16:45:00', -67.45, 'TARGET #1234' UNION ALL
    SELECT TIMESTAMP '2025-06-23 14:15:00', -91.23, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2025-07-02 15:30:00', -74.56, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2025-07-14 16:00:00', -102.34, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-07-24 14:45:00', -58.90, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2025-08-04 15:15:00', -89.12, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-08-12 16:30:00', -63.45, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2025-08-22 14:00:00', -76.78, 'TARGET #1234' UNION ALL
    SELECT TIMESTAMP '2025-09-03 15:45:00', -97.23, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2025-09-11 16:15:00', -54.67, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-09-21 14:30:00', -82.34, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2025-10-02 15:00:00', -71.90, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-10-14 16:45:00', -88.12, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2025-10-24 14:15:00', -59.45, 'TARGET #1234' UNION ALL
    SELECT TIMESTAMP '2025-11-03 15:30:00', -105.67, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-11-13 16:00:00', -73.23, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2025-11-23 14:45:00', -94.56, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2025-12-04 15:15:00', -112.34, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2025-12-12 16:30:00', -68.90, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2025-12-22 14:00:00', -85.12, 'TARGET #1234' UNION ALL
    SELECT TIMESTAMP '2026-01-03 15:45:00', -79.45, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2026-01-11 16:15:00', -96.78, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2026-01-21 14:30:00', -61.23, 'KROGER #567' UNION ALL
    SELECT TIMESTAMP '2026-02-02 15:00:00', -90.34, 'WHOLE FOODS MARKET #1042' UNION ALL
    SELECT TIMESTAMP '2026-02-10 16:45:00', -57.67, 'TRADER JOES #890' UNION ALL
    SELECT TIMESTAMP '2026-02-20 14:15:00', -74.12, 'KROGER #567'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Groceries' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Gas (24 entries, 2/month) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-06 08:30:00' d, -42.50 amt, 'SHELL OIL 54321' descr UNION ALL
    SELECT TIMESTAMP '2025-03-19 17:15:00', -38.75, 'CHEVRON #789' UNION ALL
    SELECT TIMESTAMP '2025-04-05 08:45:00', -45.20, 'BP #456 FUEL' UNION ALL
    SELECT TIMESTAMP '2025-04-18 17:00:00', -39.80, 'SHELL OIL 54321' UNION ALL
    SELECT TIMESTAMP '2025-05-07 08:30:00', -47.15, 'CHEVRON #789' UNION ALL
    SELECT TIMESTAMP '2025-05-21 17:15:00', -41.30, 'SHELL OIL 54321' UNION ALL
    SELECT TIMESTAMP '2025-06-04 08:45:00', -49.90, 'BP #456 FUEL' UNION ALL
    SELECT TIMESTAMP '2025-06-19 17:00:00', -44.25, 'CHEVRON #789' UNION ALL
    SELECT TIMESTAMP '2025-07-03 08:30:00', -52.10, 'SHELL OIL 54321' UNION ALL
    SELECT TIMESTAMP '2025-07-18 17:15:00', -46.75, 'BP #456 FUEL' UNION ALL
    SELECT TIMESTAMP '2025-08-06 08:45:00', -50.30, 'CHEVRON #789' UNION ALL
    SELECT TIMESTAMP '2025-08-20 17:00:00', -43.85, 'SHELL OIL 54321' UNION ALL
    SELECT TIMESTAMP '2025-09-05 08:30:00', -48.60, 'BP #456 FUEL' UNION ALL
    SELECT TIMESTAMP '2025-09-19 17:15:00', -41.90, 'CHEVRON #789' UNION ALL
    SELECT TIMESTAMP '2025-10-04 08:45:00', -45.75, 'SHELL OIL 54321' UNION ALL
    SELECT TIMESTAMP '2025-10-17 17:00:00', -39.20, 'BP #456 FUEL' UNION ALL
    SELECT TIMESTAMP '2025-11-06 08:30:00', -43.50, 'CHEVRON #789' UNION ALL
    SELECT TIMESTAMP '2025-11-20 17:15:00', -46.10, 'SHELL OIL 54321' UNION ALL
    SELECT TIMESTAMP '2025-12-05 08:45:00', -44.80, 'BP #456 FUEL' UNION ALL
    SELECT TIMESTAMP '2025-12-18 17:00:00', -40.35, 'CHEVRON #789' UNION ALL
    SELECT TIMESTAMP '2026-01-07 08:30:00', -47.90, 'SHELL OIL 54321' UNION ALL
    SELECT TIMESTAMP '2026-01-20 17:15:00', -42.15, 'BP #456 FUEL' UNION ALL
    SELECT TIMESTAMP '2026-02-04 08:45:00', -46.50, 'CHEVRON #789' UNION ALL
    SELECT TIMESTAMP '2026-02-17 17:00:00', -41.70, 'SHELL OIL 54321'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Gas' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Coffee (24 entries, 2/month) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, 'STARBUCKS #8901', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-03 07:45:00' d, -5.75 amt UNION ALL
    SELECT TIMESTAMP '2025-03-17 07:30:00', -6.25 UNION ALL
    SELECT TIMESTAMP '2025-04-02 07:45:00', -5.50 UNION ALL
    SELECT TIMESTAMP '2025-04-16 07:30:00', -6.75 UNION ALL
    SELECT TIMESTAMP '2025-05-01 07:45:00', -5.90 UNION ALL
    SELECT TIMESTAMP '2025-05-15 07:30:00', -6.10 UNION ALL
    SELECT TIMESTAMP '2025-06-03 07:45:00', -6.50 UNION ALL
    SELECT TIMESTAMP '2025-06-17 07:30:00', -5.80 UNION ALL
    SELECT TIMESTAMP '2025-07-01 07:45:00', -6.95 UNION ALL
    SELECT TIMESTAMP '2025-07-15 07:30:00', -5.45 UNION ALL
    SELECT TIMESTAMP '2025-08-04 07:45:00', -6.30 UNION ALL
    SELECT TIMESTAMP '2025-08-18 07:30:00', -5.65 UNION ALL
    SELECT TIMESTAMP '2025-09-02 07:45:00', -6.15 UNION ALL
    SELECT TIMESTAMP '2025-09-16 07:30:00', -5.85 UNION ALL
    SELECT TIMESTAMP '2025-10-01 07:45:00', -6.40 UNION ALL
    SELECT TIMESTAMP '2025-10-15 07:30:00', -5.70 UNION ALL
    SELECT TIMESTAMP '2025-11-03 07:45:00', -6.20 UNION ALL
    SELECT TIMESTAMP '2025-11-17 07:30:00', -5.95 UNION ALL
    SELECT TIMESTAMP '2025-12-01 07:45:00', -6.60 UNION ALL
    SELECT TIMESTAMP '2025-12-15 07:30:00', -5.55 UNION ALL
    SELECT TIMESTAMP '2026-01-05 07:45:00', -6.45 UNION ALL
    SELECT TIMESTAMP '2026-01-19 07:30:00', -5.80 UNION ALL
    SELECT TIMESTAMP '2026-02-02 07:45:00', -6.35 UNION ALL
    SELECT TIMESTAMP '2026-02-16 07:30:00', -5.60
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Coffee Shops' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Restaurants (24 entries, 2/month) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-07 19:30:00' d, -42.80 amt, 'OLIVE GARDEN #234' descr UNION ALL
    SELECT TIMESTAMP '2025-03-21 20:00:00', -35.50, 'SUSHI PALACE' UNION ALL
    SELECT TIMESTAMP '2025-04-06 19:15:00', -58.90, 'THE CHEESECAKE FACTORY' UNION ALL
    SELECT TIMESTAMP '2025-04-20 20:30:00', -38.25, 'OLIVE GARDEN #234' UNION ALL
    SELECT TIMESTAMP '2025-05-04 19:45:00', -45.60, 'PANERA BREAD #456' UNION ALL
    SELECT TIMESTAMP '2025-05-18 20:00:00', -62.30, 'SUSHI PALACE' UNION ALL
    SELECT TIMESTAMP '2025-06-01 19:30:00', -48.75, 'OLIVE GARDEN #234' UNION ALL
    SELECT TIMESTAMP '2025-06-15 20:15:00', -55.40, 'THE LOCAL BREWERY' UNION ALL
    SELECT TIMESTAMP '2025-07-06 19:00:00', -41.90, 'PANERA BREAD #456' UNION ALL
    SELECT TIMESTAMP '2025-07-20 20:30:00', -67.25, 'THE CHEESECAKE FACTORY' UNION ALL
    SELECT TIMESTAMP '2025-08-03 19:45:00', -52.10, 'SUSHI PALACE' UNION ALL
    SELECT TIMESTAMP '2025-08-17 20:00:00', -44.35, 'OLIVE GARDEN #234' UNION ALL
    SELECT TIMESTAMP '2025-09-07 19:15:00', -59.80, 'THE LOCAL BREWERY' UNION ALL
    SELECT TIMESTAMP '2025-09-21 20:45:00', -36.90, 'PANERA BREAD #456' UNION ALL
    SELECT TIMESTAMP '2025-10-05 19:30:00', -71.20, 'THE CHEESECAKE FACTORY' UNION ALL
    SELECT TIMESTAMP '2025-10-19 20:00:00', -43.50, 'OLIVE GARDEN #234' UNION ALL
    SELECT TIMESTAMP '2025-11-02 19:45:00', -56.75, 'SUSHI PALACE' UNION ALL
    SELECT TIMESTAMP '2025-11-16 20:15:00', -49.30, 'THE LOCAL BREWERY' UNION ALL
    SELECT TIMESTAMP '2025-12-07 19:00:00', -65.45, 'OLIVE GARDEN #234' UNION ALL
    SELECT TIMESTAMP '2025-12-21 20:30:00', -78.90, 'THE CHEESECAKE FACTORY' UNION ALL
    SELECT TIMESTAMP '2026-01-04 19:45:00', -47.60, 'PANERA BREAD #456' UNION ALL
    SELECT TIMESTAMP '2026-01-18 20:00:00', -53.25, 'SUSHI PALACE' UNION ALL
    SELECT TIMESTAMP '2026-02-01 19:15:00', -44.80, 'OLIVE GARDEN #234' UNION ALL
    SELECT TIMESTAMP '2026-02-15 20:30:00', -61.50, 'THE LOCAL BREWERY'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Restaurants' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Netflix (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -15.99, 'NETFLIX.COM', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-15 00:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-15 00:00:00'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Streaming Services' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Spotify (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -10.99, 'SPOTIFY USA', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-15 00:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-15 00:00:00'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Streaming Services' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Disney+ (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -13.99, 'DISNEY PLUS', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-15 00:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-15 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-15 00:00:00'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Streaming Services' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Phone - Verizon (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -85.00, 'VERIZON WIRELESS', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-20 00:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-20 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-20 00:00:00'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Phone' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Internet - Comcast (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -75.00, 'COMCAST CABLE COMM', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-12 00:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-12 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-12 00:00:00'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Internet' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Gym (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -24.99, 'PLANET FITNESS MONTHLY', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-01 00:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-01 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-01 00:00:00'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Gym Membership' AND c.user_id IS NULL;

-- ----- CREDIT CARD: iCloud (12 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, -2.99, 'APPLE.COM/BILL', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-03 00:00:00' d UNION ALL
    SELECT TIMESTAMP '2025-04-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-05-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-06-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-07-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-08-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-09-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-10-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-11-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2025-12-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-01-03 00:00:00' UNION ALL
    SELECT TIMESTAMP '2026-02-03 00:00:00'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Cloud Storage' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Fast Food (12 entries, 1/month) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-11 12:15:00' d, -12.49 amt, 'MCDONALD''S #5678' descr UNION ALL
    SELECT TIMESTAMP '2025-04-09 12:30:00', -14.25, 'CHIPOTLE ONLINE ORD' UNION ALL
    SELECT TIMESTAMP '2025-05-13 12:00:00', -11.80, 'MCDONALD''S #5678' UNION ALL
    SELECT TIMESTAMP '2025-06-08 12:45:00', -15.30, 'CHIPOTLE ONLINE ORD' UNION ALL
    SELECT TIMESTAMP '2025-07-10 12:15:00', -10.95, 'WENDY''S #3456' UNION ALL
    SELECT TIMESTAMP '2025-08-14 12:30:00', -13.50, 'MCDONALD''S #5678' UNION ALL
    SELECT TIMESTAMP '2025-09-09 12:00:00', -16.20, 'CHIPOTLE ONLINE ORD' UNION ALL
    SELECT TIMESTAMP '2025-10-08 12:45:00', -11.45, 'WENDY''S #3456' UNION ALL
    SELECT TIMESTAMP '2025-11-11 12:15:00', -14.75, 'MCDONALD''S #5678' UNION ALL
    SELECT TIMESTAMP '2025-12-09 12:30:00', -12.90, 'CHIPOTLE ONLINE ORD' UNION ALL
    SELECT TIMESTAMP '2026-01-08 12:00:00', -15.60, 'BURGER KING #7890' UNION ALL
    SELECT TIMESTAMP '2026-02-11 12:45:00', -13.25, 'MCDONALD''S #5678'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Fast Food' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Amazon (12 entries, 1/month) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-09 10:00:00' d, -67.82 amt, 'AMAZON.COM*1A2B3C4D' descr UNION ALL
    SELECT TIMESTAMP '2025-04-14 10:00:00', -34.99, 'AMAZON.COM*5E6F7G8H' UNION ALL
    SELECT TIMESTAMP '2025-05-08 10:00:00', -89.45, 'AMAZON.COM*9I0J1K2L' UNION ALL
    SELECT TIMESTAMP '2025-06-11 10:00:00', -22.50, 'AMAZON.COM*3M4N5O6P' UNION ALL
    SELECT TIMESTAMP '2025-07-16 10:00:00', -156.78, 'AMAZON.COM*7Q8R9S0T' UNION ALL
    SELECT TIMESTAMP '2025-08-09 10:00:00', -45.30, 'AMAZON.COM*1U2V3W4X' UNION ALL
    SELECT TIMESTAMP '2025-09-13 10:00:00', -73.25, 'AMAZON.COM*5Y6Z7A8B' UNION ALL
    SELECT TIMESTAMP '2025-10-11 10:00:00', -28.99, 'AMAZON.COM*9C0D1E2F' UNION ALL
    SELECT TIMESTAMP '2025-11-08 10:00:00', -112.50, 'AMAZON.COM*3G4H5I6J' UNION ALL
    SELECT TIMESTAMP '2025-12-14 10:00:00', -198.75, 'AMAZON.COM*7K8L9M0N' UNION ALL
    SELECT TIMESTAMP '2026-01-10 10:00:00', -54.20, 'AMAZON.COM*1O2P3Q4R' UNION ALL
    SELECT TIMESTAMP '2026-02-08 10:00:00', -41.99, 'AMAZON.COM*5S6T7U8V'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Amazon' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Food Delivery (8 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-22 19:45:00' d, -28.45 amt, 'DOORDASH*ORDER 12345' descr UNION ALL
    SELECT TIMESTAMP '2025-05-16 20:00:00', -34.80, 'UBER *EATS ORDER' UNION ALL
    SELECT TIMESTAMP '2025-06-28 19:30:00', -31.25, 'DOORDASH*ORDER 67890' UNION ALL
    SELECT TIMESTAMP '2025-08-08 20:15:00', -27.90, 'GRUBHUB ORDER #111' UNION ALL
    SELECT TIMESTAMP '2025-09-26 19:45:00', -36.50, 'UBER *EATS ORDER' UNION ALL
    SELECT TIMESTAMP '2025-11-14 20:00:00', -42.15, 'DOORDASH*ORDER 24680' UNION ALL
    SELECT TIMESTAMP '2025-12-27 19:30:00', -29.75, 'GRUBHUB ORDER #222' UNION ALL
    SELECT TIMESTAMP '2026-02-07 20:15:00', -33.20, 'UBER *EATS ORDER'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Food Delivery' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Clothing (6 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-04-12 14:30:00' d, -89.99 amt, 'NORDSTROM #456' descr UNION ALL
    SELECT TIMESTAMP '2025-06-21 15:00:00', -45.50, 'H&M CLOTHING' UNION ALL
    SELECT TIMESTAMP '2025-08-16 14:45:00', -67.25, 'OLD NAVY #789' UNION ALL
    SELECT TIMESTAMP '2025-10-25 15:15:00', -125.00, 'NORDSTROM #456' UNION ALL
    SELECT TIMESTAMP '2025-12-20 14:00:00', -78.50, 'H&M CLOTHING' UNION ALL
    SELECT TIMESTAMP '2026-02-14 15:30:00', -52.75, 'OLD NAVY #789'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Clothing' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Rideshare (6 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-28 22:30:00' d, -18.45 amt, 'UBER *TRIP 4567' descr UNION ALL
    SELECT TIMESTAMP '2025-05-24 21:45:00', -24.80, 'LYFT *RIDE 8901' UNION ALL
    SELECT TIMESTAMP '2025-07-19 22:00:00', -16.50, 'UBER *TRIP 2345' UNION ALL
    SELECT TIMESTAMP '2025-09-13 21:30:00', -22.75, 'LYFT *RIDE 6789' UNION ALL
    SELECT TIMESTAMP '2025-11-22 22:15:00', -28.90, 'UBER *TRIP 0123' UNION ALL
    SELECT TIMESTAMP '2026-01-31 21:45:00', -19.60, 'LYFT *RIDE 4567'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Rideshare' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Haircuts (6 entries, every 2 months) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, 'GREAT CLIPS #234', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-15 11:00:00' d, -28.00 amt UNION ALL
    SELECT TIMESTAMP '2025-05-17 11:00:00', -28.00 UNION ALL
    SELECT TIMESTAMP '2025-07-19 11:00:00', -32.00 UNION ALL
    SELECT TIMESTAMP '2025-09-20 11:00:00', -32.00 UNION ALL
    SELECT TIMESTAMP '2025-11-15 11:00:00', -32.00 UNION ALL
    SELECT TIMESTAMP '2026-01-17 11:00:00', -32.00
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Haircuts' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Pet Food (8 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, 'PETCO #567', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-14 14:00:00' d, -45.99 amt UNION ALL
    SELECT TIMESTAMP '2025-05-10 14:30:00', -52.30 UNION ALL
    SELECT TIMESTAMP '2025-06-14 14:00:00', -48.75 UNION ALL
    SELECT TIMESTAMP '2025-08-09 14:30:00', -55.20 UNION ALL
    SELECT TIMESTAMP '2025-09-13 14:00:00', -47.50 UNION ALL
    SELECT TIMESTAMP '2025-11-08 14:30:00', -51.80 UNION ALL
    SELECT TIMESTAMP '2025-12-13 14:00:00', -49.99 UNION ALL
    SELECT TIMESTAMP '2026-02-07 14:30:00', -53.45
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Pet Food' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Vet (3 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, 'BANFIELD PET HOSPITAL', c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-04-22 10:00:00' d, -185.00 amt UNION ALL
    SELECT TIMESTAMP '2025-08-19 10:00:00', -95.00 UNION ALL
    SELECT TIMESTAMP '2025-12-16 10:00:00', -210.00
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Vet' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Gifts (6 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-05-10 13:00:00' d, -65.00 amt, 'TARGET #1234 GIFT' descr UNION ALL
    SELECT TIMESTAMP '2025-06-14 13:30:00', -45.00, 'HALLMARK #890' UNION ALL
    SELECT TIMESTAMP '2025-09-28 13:00:00', -35.50, 'ETSY.COM*SELLER123' UNION ALL
    SELECT TIMESTAMP '2025-11-28 13:30:00', -120.00, 'BEST BUY #890 GIFT' UNION ALL
    SELECT TIMESTAMP '2025-12-18 13:00:00', -185.50, 'NORDSTROM #456 GIFT' UNION ALL
    SELECT TIMESTAMP '2026-02-13 13:30:00', -75.00, 'HALLMARK #890'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Gifts' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Medical (3 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-04-15 09:30:00' d, -125.00 amt, 'DR SMITH FAMILY MEDICINE' descr UNION ALL
    SELECT TIMESTAMP '2025-08-20 09:00:00', -85.00, 'QUEST DIAGNOSTICS' UNION ALL
    SELECT TIMESTAMP '2025-12-10 09:30:00', -150.00, 'DR SMITH FAMILY MEDICINE'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Medical' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Pharmacy (4 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, FALSE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-04-18 16:00:00' d, -15.80 amt, 'CVS/PHARMACY #789' descr UNION ALL
    SELECT TIMESTAMP '2025-07-22 16:30:00', -22.45, 'WALGREENS #456' UNION ALL
    SELECT TIMESTAMP '2025-10-14 16:00:00', -18.90, 'CVS/PHARMACY #789' UNION ALL
    SELECT TIMESTAMP '2026-01-20 16:30:00', -12.50, 'WALGREENS #456'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Pharmacy' AND c.user_id IS NULL;

-- ----- CREDIT CARD: Date Night (custom category, 6 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-03-28 20:00:00' d, -95.50 amt, 'RUTH''S CHRIS STEAK HOUSE' descr UNION ALL
    SELECT TIMESTAMP '2025-05-23 20:30:00', -82.00, 'THE MELTING POT' UNION ALL
    SELECT TIMESTAMP '2025-07-25 20:00:00', -110.25, 'NOBU RESTAURANT' UNION ALL
    SELECT TIMESTAMP '2025-09-26 20:30:00', -78.50, 'CAPITAL GRILLE' UNION ALL
    SELECT TIMESTAMP '2025-11-28 20:00:00', -125.75, 'RUTH''S CHRIS STEAK HOUSE' UNION ALL
    SELECT TIMESTAMP '2026-02-14 20:30:00', -135.00, 'NOBU RESTAURANT'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Date Night' AND c.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com');

-- ----- CREDIT CARD: Travel (1 summer trip) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-07-28 10:00:00' d, -385.00 amt, 'UNITED AIRLINES #UA1234' descr, 'Flights' cat UNION ALL
    SELECT TIMESTAMP '2025-08-01 15:00:00', -189.00, 'MARRIOTT HOTEL CHICAGO', 'Hotels' UNION ALL
    SELECT TIMESTAMP '2025-08-02 15:00:00', -189.00, 'MARRIOTT HOTEL CHICAGO', 'Hotels' UNION ALL
    SELECT TIMESTAMP '2025-08-03 15:00:00', -189.00, 'MARRIOTT HOTEL CHICAGO', 'Hotels'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = v.cat AND c.user_id IS NULL;

-- ----- CREDIT CARD: Electronics (2 entries) -----
INSERT INTO transactions (account_id, posted_at, amount, description, category_id, is_manually_categorized, created_at, updated_at)
SELECT a.id, v.d, v.amt, v.descr, c.id, TRUE, v.d, v.d
FROM (
    SELECT TIMESTAMP '2025-06-15 13:00:00' d, -249.99 amt, 'BEST BUY #890' descr UNION ALL
    SELECT TIMESTAMP '2025-11-29 13:30:00', -179.99, 'BEST BUY #890'
) v
JOIN accounts a ON a.name = 'Visa Rewards Card' AND a.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
JOIN categories c ON c.name = 'Electronics' AND c.user_id IS NULL;

-- ============================================================
-- 6. LINK TRANSFER PAIRS
-- ============================================================

-- Link checking→savings: set transfer_pair_id on the checking side
UPDATE transactions
SET transfer_pair_id = (
    SELECT t2.id FROM transactions t2
    JOIN accounts a2 ON t2.account_id = a2.id
    WHERE a2.name = 'Emergency Savings'
    AND a2.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
    AND t2.description = 'TRANSFER FROM PRIMARY CHECKING'
    AND CAST(t2.posted_at AS DATE) = CAST(transactions.posted_at AS DATE)
)
WHERE transactions.description = 'TRANSFER TO EMERGENCY SAVINGS'
AND transactions.account_id IN (
    SELECT id FROM accounts WHERE name = 'Primary Checking'
    AND user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
);

-- Link checking→savings: set transfer_pair_id on the savings side
UPDATE transactions
SET transfer_pair_id = (
    SELECT t2.id FROM transactions t2
    JOIN accounts a2 ON t2.account_id = a2.id
    WHERE a2.name = 'Primary Checking'
    AND a2.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
    AND t2.description = 'TRANSFER TO EMERGENCY SAVINGS'
    AND CAST(t2.posted_at AS DATE) = CAST(transactions.posted_at AS DATE)
)
WHERE transactions.description = 'TRANSFER FROM PRIMARY CHECKING'
AND transactions.account_id IN (
    SELECT id FROM accounts WHERE name = 'Emergency Savings'
    AND user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
);

-- Link checking→credit card: set transfer_pair_id on the checking side
UPDATE transactions
SET transfer_pair_id = (
    SELECT t2.id FROM transactions t2
    JOIN accounts a2 ON t2.account_id = a2.id
    WHERE a2.name = 'Visa Rewards Card'
    AND a2.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
    AND t2.description = 'PAYMENT RECEIVED - THANK YOU'
    AND CAST(t2.posted_at AS DATE) = CAST(transactions.posted_at AS DATE)
)
WHERE transactions.description = 'VISA REWARDS CARD PAYMENT'
AND transactions.account_id IN (
    SELECT id FROM accounts WHERE name = 'Primary Checking'
    AND user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
);

-- Link checking→credit card: set transfer_pair_id on the credit card side
UPDATE transactions
SET transfer_pair_id = (
    SELECT t2.id FROM transactions t2
    JOIN accounts a2 ON t2.account_id = a2.id
    WHERE a2.name = 'Primary Checking'
    AND a2.user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
    AND t2.description = 'VISA REWARDS CARD PAYMENT'
    AND CAST(t2.posted_at AS DATE) = CAST(transactions.posted_at AS DATE)
)
WHERE transactions.description = 'PAYMENT RECEIVED - THANK YOU'
AND transactions.account_id IN (
    SELECT id FROM accounts WHERE name = 'Visa Rewards Card'
    AND user_id = (SELECT id FROM users WHERE email = 'example.user@test.com')
);

-- ============================================================
-- 7. BUDGET TARGETS (6 months: Sep 2025 – Feb 2026)
-- ============================================================

INSERT INTO budget_targets (user_id, month_key, category_id, target_amount, created_at, updated_at)
SELECT u.id, v.mk, c.id, v.amt, TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2025-09-01 10:00:00'
FROM users u
CROSS JOIN (
    SELECT '2025-09' mk, 'Groceries' cat, 500.00 amt UNION ALL
    SELECT '2025-09', 'Restaurants', 200.00 UNION ALL
    SELECT '2025-09', 'Coffee Shops', 50.00 UNION ALL
    SELECT '2025-09', 'Fast Food', 40.00 UNION ALL
    SELECT '2025-09', 'Food Delivery', 50.00 UNION ALL
    SELECT '2025-09', 'Gas', 120.00 UNION ALL
    SELECT '2025-09', 'Streaming Services', 50.00 UNION ALL
    SELECT '2025-09', 'Clothing', 100.00 UNION ALL
    SELECT '2025-10', 'Groceries', 500.00 UNION ALL
    SELECT '2025-10', 'Restaurants', 200.00 UNION ALL
    SELECT '2025-10', 'Coffee Shops', 50.00 UNION ALL
    SELECT '2025-10', 'Fast Food', 40.00 UNION ALL
    SELECT '2025-10', 'Food Delivery', 50.00 UNION ALL
    SELECT '2025-10', 'Gas', 120.00 UNION ALL
    SELECT '2025-10', 'Streaming Services', 50.00 UNION ALL
    SELECT '2025-10', 'Clothing', 100.00 UNION ALL
    SELECT '2025-11', 'Groceries', 550.00 UNION ALL
    SELECT '2025-11', 'Restaurants', 250.00 UNION ALL
    SELECT '2025-11', 'Coffee Shops', 50.00 UNION ALL
    SELECT '2025-11', 'Fast Food', 40.00 UNION ALL
    SELECT '2025-11', 'Food Delivery', 50.00 UNION ALL
    SELECT '2025-11', 'Gas', 120.00 UNION ALL
    SELECT '2025-11', 'Streaming Services', 50.00 UNION ALL
    SELECT '2025-11', 'Clothing', 150.00 UNION ALL
    SELECT '2025-12', 'Groceries', 600.00 UNION ALL
    SELECT '2025-12', 'Restaurants', 300.00 UNION ALL
    SELECT '2025-12', 'Coffee Shops', 50.00 UNION ALL
    SELECT '2025-12', 'Fast Food', 40.00 UNION ALL
    SELECT '2025-12', 'Food Delivery', 50.00 UNION ALL
    SELECT '2025-12', 'Gas', 120.00 UNION ALL
    SELECT '2025-12', 'Streaming Services', 50.00 UNION ALL
    SELECT '2025-12', 'Clothing', 200.00 UNION ALL
    SELECT '2026-01', 'Groceries', 500.00 UNION ALL
    SELECT '2026-01', 'Restaurants', 200.00 UNION ALL
    SELECT '2026-01', 'Coffee Shops', 50.00 UNION ALL
    SELECT '2026-01', 'Fast Food', 40.00 UNION ALL
    SELECT '2026-01', 'Food Delivery', 50.00 UNION ALL
    SELECT '2026-01', 'Gas', 120.00 UNION ALL
    SELECT '2026-01', 'Streaming Services', 50.00 UNION ALL
    SELECT '2026-01', 'Clothing', 100.00 UNION ALL
    SELECT '2026-02', 'Groceries', 500.00 UNION ALL
    SELECT '2026-02', 'Restaurants', 200.00 UNION ALL
    SELECT '2026-02', 'Coffee Shops', 50.00 UNION ALL
    SELECT '2026-02', 'Fast Food', 40.00 UNION ALL
    SELECT '2026-02', 'Food Delivery', 50.00 UNION ALL
    SELECT '2026-02', 'Gas', 120.00 UNION ALL
    SELECT '2026-02', 'Streaming Services', 50.00 UNION ALL
    SELECT '2026-02', 'Clothing', 100.00
) v
JOIN categories c ON c.name = v.cat AND c.user_id IS NULL
WHERE u.email = 'example.user@test.com';

-- ============================================================
-- 8. RECURRING PATTERNS
-- ============================================================

-- Salary (bi-weekly)
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'ACME Corp Payroll', 'PAYROLL - ACME CORP', 3200.00, 0, 'BIWEEKLY', 1, DATE '2026-03-01', c.id, FALSE, TRUE, TIMESTAMP '2026-02-15 07:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-15 07:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Salary' AND c.user_id IS NULL;

-- Rent
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Oakwood Apartments', 'RENT PAYMENT - OAKWOOD', 1650.00, 0, 'MONTHLY', 1, DATE '2026-03-01', c.id, TRUE, TRUE, TIMESTAMP '2026-02-01 09:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-01 09:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Rent/Mortgage' AND c.user_id IS NULL;

-- Netflix
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Netflix', 'NETFLIX.COM', 15.99, 0, 'MONTHLY', 15, DATE '2026-03-15', c.id, TRUE, TRUE, TIMESTAMP '2026-02-15 00:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-15 00:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Streaming Services' AND c.user_id IS NULL;

-- Spotify
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Spotify', 'SPOTIFY USA', 10.99, 0, 'MONTHLY', 15, DATE '2026-03-15', c.id, TRUE, TRUE, TIMESTAMP '2026-02-15 00:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-15 00:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Streaming Services' AND c.user_id IS NULL;

-- Disney+
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Disney Plus', 'DISNEY PLUS', 13.99, 0, 'MONTHLY', 15, DATE '2026-03-15', c.id, TRUE, TRUE, TIMESTAMP '2026-02-15 00:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-15 00:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Streaming Services' AND c.user_id IS NULL;

-- Verizon
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Verizon Wireless', 'VERIZON WIRELESS', 85.00, 0, 'MONTHLY', 20, DATE '2026-03-20', c.id, TRUE, TRUE, TIMESTAMP '2026-02-20 00:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-20 00:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Phone' AND c.user_id IS NULL;

-- Comcast
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Comcast Internet', 'COMCAST CABLE COMM', 75.00, 0, 'MONTHLY', 12, DATE '2026-03-12', c.id, TRUE, TRUE, TIMESTAMP '2026-02-12 00:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-12 00:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Internet' AND c.user_id IS NULL;

-- Planet Fitness
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Planet Fitness', 'PLANET FITNESS', 24.99, 0, 'MONTHLY', 1, DATE '2026-03-01', c.id, TRUE, TRUE, TIMESTAMP '2026-02-01 00:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-01 00:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Gym Membership' AND c.user_id IS NULL;

-- iCloud
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Apple iCloud', 'APPLE.COM/BILL', 2.99, 0, 'MONTHLY', 3, DATE '2026-03-03', c.id, TRUE, TRUE, TIMESTAMP '2026-02-03 00:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-03 00:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Cloud Storage' AND c.user_id IS NULL;

-- Health Insurance
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Blue Cross Health', 'BLUE CROSS HEALTH', 320.00, 0, 'MONTHLY', 1, DATE '2026-03-01', c.id, TRUE, TRUE, TIMESTAMP '2026-02-01 08:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-01 08:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Health Insurance' AND c.user_id IS NULL;

-- Car Insurance
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'State Farm Auto', 'STATE FARM AUTO', 145.00, 0, 'MONTHLY', 10, DATE '2026-03-10', c.id, TRUE, TRUE, TIMESTAMP '2026-02-10 09:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-10 09:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Auto Insurance' AND c.user_id IS NULL;

-- Car Loan
INSERT INTO recurring_patterns (user_id, name, merchant_pattern, expected_amount, amount_variance, frequency, day_of_month, next_expected_date, category_id, is_bill, is_active, last_occurrence_at, created_at, updated_at)
SELECT u.id, 'Capital One Auto Loan', 'CAPITAL ONE AUTO', 385.00, 0, 'MONTHLY', 15, DATE '2026-03-15', c.id, TRUE, TRUE, TIMESTAMP '2026-02-15 09:00:00', TIMESTAMP '2025-09-01 10:00:00', TIMESTAMP '2026-02-15 09:00:00'
FROM users u, categories c WHERE u.email = 'example.user@test.com' AND c.name = 'Loan Payments' AND c.user_id = u.id;
