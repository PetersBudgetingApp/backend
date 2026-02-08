# Rule Backfill Must Uncategorize On Unmatch

- Date: 2026-02-08
- Area: transactions
- Tags: categorization-rules, backfill, uncategorized

## Symptom
Updating a rule so older auto-categorized transactions no longer matched would clear the rule-tracking link but keep the old category on those transactions.

## Root Cause
`TransactionService.backfillCategorizationRules` only cleared `categorizedByRuleId` in the no-match branch and did not clear `categoryId`.

## Correct Fix
When a non-manual transaction previously categorized by rule no longer matches, clear both `categorizedByRuleId` and `categoryId` (and keep `manuallyCategorized=false`) in the same save.

## Verification
- `mvn -q -Dtest=TransactionServiceTest test`
- `mvn -q -Dtest=CategorizationRuleServiceTest test`

## References
- `src/main/java/com/peter/budget/service/TransactionService.java:108`
- `src/test/java/com/peter/budget/service/TransactionServiceTest.java:286`
