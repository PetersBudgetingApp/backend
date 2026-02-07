CREATE TABLE category_overrides (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    parent_id_override BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    name_override VARCHAR(100) NOT NULL,
    icon_override VARCHAR(50),
    color_override VARCHAR(20),
    category_type_override VARCHAR(20) NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_category_overrides_user_category UNIQUE (user_id, category_id)
);

CREATE INDEX idx_category_overrides_user_id ON category_overrides(user_id);
CREATE INDEX idx_category_overrides_category_id ON category_overrides(category_id);
