-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- SimpleFin connections table
CREATE TABLE simplefin_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_url_encrypted VARCHAR(1024) NOT NULL,
    institution_name VARCHAR(255),
    last_sync_at TIMESTAMP,
    sync_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    requests_today INTEGER NOT NULL DEFAULT 0,
    requests_reset_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_simplefin_connections_user_id ON simplefin_connections(user_id);

-- Accounts table
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    connection_id BIGINT REFERENCES simplefin_connections(id) ON DELETE SET NULL,
    external_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    institution_name VARCHAR(255),
    account_type VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    current_balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    available_balance DECIMAL(19, 4),
    balance_updated_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_account_external_id UNIQUE (connection_id, external_id)
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_connection_id ON accounts(connection_id);

-- Categories table (hierarchical)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES categories(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(20),
    category_type VARCHAR(20) NOT NULL DEFAULT 'EXPENSE',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);

-- Recurring patterns table
CREATE TABLE recurring_patterns (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    merchant_pattern VARCHAR(255),
    expected_amount DECIMAL(19, 4),
    amount_variance DECIMAL(19, 4) DEFAULT 0,
    frequency VARCHAR(20) NOT NULL,
    day_of_week INTEGER,
    day_of_month INTEGER,
    next_expected_date DATE,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    is_bill BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_occurrence_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recurring_patterns_user_id ON recurring_patterns(user_id);
CREATE INDEX idx_recurring_patterns_next_expected ON recurring_patterns(next_expected_date);

-- Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    external_id VARCHAR(255),
    posted_at TIMESTAMP NOT NULL,
    transacted_at TIMESTAMP,
    amount DECIMAL(19, 4) NOT NULL,
    pending BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500),
    payee VARCHAR(255),
    memo TEXT,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    is_manually_categorized BOOLEAN NOT NULL DEFAULT FALSE,
    transfer_pair_id BIGINT REFERENCES transactions(id) ON DELETE SET NULL,
    is_internal_transfer BOOLEAN NOT NULL DEFAULT FALSE,
    exclude_from_totals BOOLEAN NOT NULL DEFAULT FALSE,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_pattern_id BIGINT REFERENCES recurring_patterns(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_transaction_external_id UNIQUE (account_id, external_id)
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_posted_at ON transactions(posted_at);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);
CREATE INDEX idx_transactions_transfer_pair_id ON transactions(transfer_pair_id);
CREATE INDEX idx_transactions_recurring_pattern_id ON transactions(recurring_pattern_id);
CREATE INDEX idx_transactions_amount ON transactions(amount);

-- Categorization rules table
CREATE TABLE categorization_rules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    pattern VARCHAR(500) NOT NULL,
    pattern_type VARCHAR(20) NOT NULL DEFAULT 'CONTAINS',
    match_field VARCHAR(20) NOT NULL DEFAULT 'DESCRIPTION',
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    priority INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categorization_rules_user_id ON categorization_rules(user_id);
CREATE INDEX idx_categorization_rules_priority ON categorization_rules(priority DESC);

-- Rate limit tracking for SimpleFin API
CREATE TABLE api_rate_limits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    api_name VARCHAR(50) NOT NULL,
    requests_count INTEGER NOT NULL DEFAULT 0,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rate_limit_user_api UNIQUE (user_id, api_name, window_start)
);

CREATE INDEX idx_api_rate_limits_user_api ON api_rate_limits(user_id, api_name);

-- Refresh tokens table for JWT
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
