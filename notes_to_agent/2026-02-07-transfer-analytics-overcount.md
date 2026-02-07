# Transfer Analytics Overcount From Link Save Clobber

- Date: 2026-02-07
- Area: analytics
- Tags: transfers, dashboard, analytics, sync

## Symptom
Dashboard income and spending totals were inflated by large internal account transfers, making monthly spending/income trend values unusable for transfer-heavy users.

## Root Cause
`TransferDetectionService.linkAsTransfer` first called `linkTransferPair(...)` to set transfer flags in SQL, then immediately called `save(tx)` for category assignment using stale in-memory `Transaction` objects that still had `transfer_pair_id=null`, `is_internal_transfer=false`, and `exclude_from_totals=false`. That update silently reverted transfer flags. Analytics then counted those rows as normal income/expense.

## Correct Fix
Set transfer linkage/flags on in-memory transactions before any `save(tx)` call, then persist category changes. Also harden analytics SQL transfer filtering to exclude internal-transfer rows and rows categorized as `TRANSFER`, and run startup transfer backfill in local profile so old data gets repaired automatically.

## Verification
- `mvn test`
- `mvn -DskipTests package`
- Manual API/UI check: dashboard cashflow/trends exclude mirrored internal transfers after restart/sync.

## References
- `/Users/petergelgor/Documents/projects/budgeting_app/backend/src/main/java/com/peter/budget/service/TransferDetectionService.java`
- `/Users/petergelgor/Documents/projects/budgeting_app/backend/src/main/java/com/peter/budget/repository/TransactionAnalyticsRepository.java`
- `/Users/petergelgor/Documents/projects/budgeting_app/backend/src/main/java/com/peter/budget/service/TransferBackfillStartupService.java`
