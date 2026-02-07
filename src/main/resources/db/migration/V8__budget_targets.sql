CREATE TABLE budget_targets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    month_key VARCHAR(7) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    target_amount DECIMAL(19, 4) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_budget_targets_user_month_category UNIQUE (user_id, month_key, category_id)
);

CREATE INDEX idx_budget_targets_user_month ON budget_targets(user_id, month_key);
