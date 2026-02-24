UPDATE categories
SET
    category_type = 'UNCATEGORIZED',
    parent_id = NULL,
    sort_order = 999,
    updated_at = CURRENT_TIMESTAMP
WHERE id = (
    SELECT id
    FROM categories
    WHERE user_id IS NULL
      AND UPPER(name) = 'UNCATEGORIZED'
    ORDER BY sort_order, id
    LIMIT 1
);

UPDATE categories
SET
    parent_id = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE parent_id = (
    SELECT id
    FROM categories
    WHERE user_id IS NULL
      AND UPPER(name) = 'UNCATEGORIZED'
    ORDER BY sort_order, id
    LIMIT 1
);

UPDATE category_overrides
SET
    parent_id_override = NULL,
    name_override = 'Uncategorized',
    icon_override = 'help-circle',
    color_override = '#9CA3AF',
    category_type_override = 'UNCATEGORIZED',
    is_hidden = FALSE
WHERE category_id = (
    SELECT id
    FROM categories
    WHERE user_id IS NULL
      AND UPPER(name) = 'UNCATEGORIZED'
    ORDER BY sort_order, id
    LIMIT 1
);

UPDATE category_overrides
SET
    parent_id_override = NULL
WHERE parent_id_override = (
    SELECT id
    FROM categories
    WHERE user_id IS NULL
      AND UPPER(name) = 'UNCATEGORIZED'
    ORDER BY sort_order, id
    LIMIT 1
);

UPDATE transactions
SET
    category_id = (
        SELECT id
        FROM categories
        WHERE user_id IS NULL
          AND UPPER(name) = 'UNCATEGORIZED'
        ORDER BY sort_order, id
        LIMIT 1
    ),
    categorized_by_rule_id = NULL,
    is_manually_categorized = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE
    category_id IS NULL
    AND EXISTS (
        SELECT 1
        FROM categories
        WHERE user_id IS NULL
          AND UPPER(name) = 'UNCATEGORIZED'
    );
