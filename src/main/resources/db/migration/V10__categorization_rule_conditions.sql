ALTER TABLE categorization_rules
ADD COLUMN condition_operator VARCHAR(10) NOT NULL DEFAULT 'AND';

ALTER TABLE categorization_rules
ADD COLUMN conditions_json TEXT;
