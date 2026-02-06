-- System-wide default categories (user_id is NULL for system categories)

-- Income categories
INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Income', 'income', '#22C55E', 'INCOME', TRUE, 1);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Salary', 'briefcase', '#22C55E', 'INCOME', TRUE, 1
FROM categories WHERE name = 'Income' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Freelance', 'laptop', '#22C55E', 'INCOME', TRUE, 2
FROM categories WHERE name = 'Income' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Investments', 'trending-up', '#22C55E', 'INCOME', TRUE, 3
FROM categories WHERE name = 'Income' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Refunds', 'rotate-ccw', '#22C55E', 'INCOME', TRUE, 4
FROM categories WHERE name = 'Income' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Other Income', 'plus-circle', '#22C55E', 'INCOME', TRUE, 5
FROM categories WHERE name = 'Income' AND user_id IS NULL;

-- Transfer category
INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Transfers', 'arrow-left-right', '#6B7280', 'TRANSFER', TRUE, 2);

-- Expense categories
INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Housing', 'home', '#3B82F6', 'EXPENSE', TRUE, 3);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Rent/Mortgage', 'key', '#3B82F6', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Housing' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Utilities', 'zap', '#3B82F6', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Housing' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Home Insurance', 'shield', '#3B82F6', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Housing' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Home Maintenance', 'wrench', '#3B82F6', 'EXPENSE', TRUE, 4
FROM categories WHERE name = 'Housing' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Transportation', 'car', '#8B5CF6', 'EXPENSE', TRUE, 4);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Gas', 'fuel', '#8B5CF6', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Transportation' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Auto Insurance', 'shield', '#8B5CF6', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Transportation' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Auto Maintenance', 'wrench', '#8B5CF6', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Transportation' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Public Transit', 'train', '#8B5CF6', 'EXPENSE', TRUE, 4
FROM categories WHERE name = 'Transportation' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Parking', 'parking', '#8B5CF6', 'EXPENSE', TRUE, 5
FROM categories WHERE name = 'Transportation' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Rideshare', 'car', '#8B5CF6', 'EXPENSE', TRUE, 6
FROM categories WHERE name = 'Transportation' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Food & Dining', 'utensils', '#F59E0B', 'EXPENSE', TRUE, 5);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Groceries', 'shopping-cart', '#F59E0B', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Food & Dining' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Restaurants', 'utensils', '#F59E0B', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Food & Dining' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Coffee Shops', 'coffee', '#F59E0B', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Food & Dining' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Fast Food', 'burger', '#F59E0B', 'EXPENSE', TRUE, 4
FROM categories WHERE name = 'Food & Dining' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Food Delivery', 'truck', '#F59E0B', 'EXPENSE', TRUE, 5
FROM categories WHERE name = 'Food & Dining' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Shopping', 'shopping-bag', '#EC4899', 'EXPENSE', TRUE, 6);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Clothing', 'shirt', '#EC4899', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Shopping' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Electronics', 'smartphone', '#EC4899', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Shopping' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Home Goods', 'home', '#EC4899', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Shopping' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Amazon', 'package', '#EC4899', 'EXPENSE', TRUE, 4
FROM categories WHERE name = 'Shopping' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Entertainment', 'film', '#EF4444', 'EXPENSE', TRUE, 7);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Streaming Services', 'tv', '#EF4444', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Entertainment' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Movies & Shows', 'film', '#EF4444', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Entertainment' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Games', 'gamepad', '#EF4444', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Entertainment' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Books', 'book', '#EF4444', 'EXPENSE', TRUE, 4
FROM categories WHERE name = 'Entertainment' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Music', 'music', '#EF4444', 'EXPENSE', TRUE, 5
FROM categories WHERE name = 'Entertainment' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Concerts & Events', 'ticket', '#EF4444', 'EXPENSE', TRUE, 6
FROM categories WHERE name = 'Entertainment' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Health & Fitness', 'heart', '#10B981', 'EXPENSE', TRUE, 8);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Gym Membership', 'dumbbell', '#10B981', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Health & Fitness' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Medical', 'stethoscope', '#10B981', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Health & Fitness' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Pharmacy', 'pill', '#10B981', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Health & Fitness' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Health Insurance', 'shield', '#10B981', 'EXPENSE', TRUE, 4
FROM categories WHERE name = 'Health & Fitness' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Subscriptions', 'repeat', '#6366F1', 'EXPENSE', TRUE, 9);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Software', 'code', '#6366F1', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Subscriptions' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Cloud Storage', 'cloud', '#6366F1', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Subscriptions' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'News & Magazines', 'newspaper', '#6366F1', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Subscriptions' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Personal Care', 'smile', '#F472B6', 'EXPENSE', TRUE, 10);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Haircuts', 'scissors', '#F472B6', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Personal Care' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Spa & Massage', 'sparkles', '#F472B6', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Personal Care' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Education', 'graduation-cap', '#0EA5E9', 'EXPENSE', TRUE, 11);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Tuition', 'school', '#0EA5E9', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Education' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Books & Supplies', 'book-open', '#0EA5E9', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Education' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Online Courses', 'monitor', '#0EA5E9', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Education' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Travel', 'plane', '#14B8A6', 'EXPENSE', TRUE, 12);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Flights', 'plane', '#14B8A6', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Travel' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Hotels', 'bed', '#14B8A6', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Travel' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Vacation', 'umbrella', '#14B8A6', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Travel' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Gifts & Donations', 'gift', '#A855F7', 'EXPENSE', TRUE, 13);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Gifts', 'gift', '#A855F7', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Gifts & Donations' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Charity', 'heart', '#A855F7', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Gifts & Donations' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Bills & Fees', 'file-text', '#64748B', 'EXPENSE', TRUE, 14);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Phone', 'phone', '#64748B', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Bills & Fees' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Internet', 'wifi', '#64748B', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Bills & Fees' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Bank Fees', 'building', '#64748B', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Bills & Fees' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Late Fees', 'alert-circle', '#64748B', 'EXPENSE', TRUE, 4
FROM categories WHERE name = 'Bills & Fees' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Pets', 'paw-print', '#84CC16', 'EXPENSE', TRUE, 15);

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Pet Food', 'bowl', '#84CC16', 'EXPENSE', TRUE, 1
FROM categories WHERE name = 'Pets' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Vet', 'stethoscope', '#84CC16', 'EXPENSE', TRUE, 2
FROM categories WHERE name = 'Pets' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
SELECT NULL, id, 'Pet Supplies', 'box', '#84CC16', 'EXPENSE', TRUE, 3
FROM categories WHERE name = 'Pets' AND user_id IS NULL;

INSERT INTO categories (user_id, parent_id, name, icon, color, category_type, is_system, sort_order)
VALUES (NULL, NULL, 'Uncategorized', 'help-circle', '#9CA3AF', 'EXPENSE', TRUE, 99);

-- Default categorization rules (system-wide)
INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Netflix', 'NETFLIX', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Streaming Services' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Spotify', 'SPOTIFY', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Streaming Services' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Disney Plus', 'DISNEY', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Streaming Services' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Amazon Prime', 'PRIME VIDEO', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Streaming Services' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Hulu', 'HULU', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Streaming Services' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Amazon', 'AMAZON', 'CONTAINS', 'DESCRIPTION', id, 50, TRUE
FROM categories WHERE name = 'Amazon' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Uber Eats', 'UBER.*EATS', 'REGEX', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Food Delivery' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'DoorDash', 'DOORDASH', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Food Delivery' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Grubhub', 'GRUBHUB', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Food Delivery' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Uber Rides', 'UBER(?!.*EATS)', 'REGEX', 'DESCRIPTION', id, 90, TRUE
FROM categories WHERE name = 'Rideshare' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Lyft', 'LYFT', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Rideshare' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Starbucks', 'STARBUCKS', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Coffee Shops' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Dunkin', 'DUNKIN', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Coffee Shops' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'McDonalds', 'MCDONALD', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Fast Food' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Wendys', 'WENDY', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Fast Food' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Burger King', 'BURGER KING', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Fast Food' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Chipotle', 'CHIPOTLE', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Fast Food' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Walmart Grocery', 'WALMART', 'CONTAINS', 'DESCRIPTION', id, 80, TRUE
FROM categories WHERE name = 'Groceries' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Target', 'TARGET', 'CONTAINS', 'DESCRIPTION', id, 70, TRUE
FROM categories WHERE name = 'Groceries' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Costco', 'COSTCO', 'CONTAINS', 'DESCRIPTION', id, 80, TRUE
FROM categories WHERE name = 'Groceries' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Whole Foods', 'WHOLE FOODS', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Groceries' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Kroger', 'KROGER', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Groceries' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Shell Gas', 'SHELL', 'CONTAINS', 'DESCRIPTION', id, 80, TRUE
FROM categories WHERE name = 'Gas' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Chevron Gas', 'CHEVRON', 'CONTAINS', 'DESCRIPTION', id, 80, TRUE
FROM categories WHERE name = 'Gas' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Exxon Gas', 'EXXON', 'CONTAINS', 'DESCRIPTION', id, 80, TRUE
FROM categories WHERE name = 'Gas' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'BP Gas', 'BP ', 'CONTAINS', 'DESCRIPTION', id, 80, TRUE
FROM categories WHERE name = 'Gas' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'CVS Pharmacy', 'CVS', 'CONTAINS', 'DESCRIPTION', id, 80, TRUE
FROM categories WHERE name = 'Pharmacy' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Walgreens', 'WALGREENS', 'CONTAINS', 'DESCRIPTION', id, 80, TRUE
FROM categories WHERE name = 'Pharmacy' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Planet Fitness', 'PLANET FITNESS', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Gym Membership' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'LA Fitness', 'LA FITNESS', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Gym Membership' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Verizon', 'VERIZON', 'CONTAINS', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Phone' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'AT&T', 'AT&T|ATT', 'REGEX', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Phone' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'T-Mobile', 'T-MOBILE|TMOBILE', 'REGEX', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Phone' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Comcast', 'COMCAST|XFINITY', 'REGEX', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Internet' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Apple iCloud', 'APPLE.COM/BILL|ICLOUD', 'REGEX', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Cloud Storage' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Google Storage', 'GOOGLE.*STORAGE|GOOGLE ONE', 'REGEX', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Cloud Storage' AND user_id IS NULL;

INSERT INTO categorization_rules (user_id, name, pattern, pattern_type, match_field, category_id, priority, is_system)
SELECT NULL, 'Payroll Direct Deposit', 'PAYROLL|DIRECT DEP|SALARY|WAGE', 'REGEX', 'DESCRIPTION', id, 100, TRUE
FROM categories WHERE name = 'Salary' AND user_id IS NULL;
