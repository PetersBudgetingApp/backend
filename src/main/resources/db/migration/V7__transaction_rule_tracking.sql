ALTER TABLE transactions
    ADD COLUMN categorized_by_rule_id BIGINT;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_categorized_by_rule
    FOREIGN KEY (categorized_by_rule_id)
    REFERENCES categorization_rules(id)
    ON DELETE SET NULL;

CREATE INDEX idx_transactions_categorized_by_rule_id ON transactions(categorized_by_rule_id);
