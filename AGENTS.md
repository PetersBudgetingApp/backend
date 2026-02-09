# Backend AGENTS.md

## Purpose
This is the complete backend operator map for the Spring API.
A new agent should be able to trace any endpoint to controller, service, repository, SQL behavior, and scheduled workflows without additional discovery.

## Repo Identity
- Path: `/Users/petergelgor/Documents/projects/budgeting_app/backend`
- Git repo: yes (independent from frontend repo)
- Stack: Spring Boot 3.5 + JDBC + Security + Flyway + WebFlux client

## Quick Start
- Run app (default profile, H2 in-memory): `mvn spring-boot:run`
- Run tests: `mvn test`
- Build jar without tests: `mvn -DskipTests package`
- Main entrypoint: `src/main/java/com/peter/budget/BudgetApplication.java`

## Profiles And Configuration

### Default (`application.properties`)
- DB: H2 in-memory (`jdbc:h2:mem:budgetdb;MODE=PostgreSQL`)
- Flyway enabled
- H2 console enabled
- JWT/encryption secrets default to randomized values if env vars absent

### Local profile (`application-local.properties`)
- DB: PostgreSQL (`DATABASE_URL`, default `jdbc:postgresql://localhost:5432/budget`)
- Uses `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- H2 console disabled
- Stable local JWT/encryption secrets can be provided via env vars
- Startup categorization backfill enabled (`app.categorization.backfill-on-startup=true`)

### Prod profile (`application-prod.properties`)
- DB from env vars only
- H2 console disabled
- JWT/encryption secrets required from env
- CORS origins from `APP_CORS_ALLOWED_ORIGINS`

## Security Model
- Main config: `src/main/java/com/peter/budget/config/SecurityConfig.java`
- JWT filter: `src/main/java/com/peter/budget/config/JwtAuthFilter.java`

### Public endpoints
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `/actuator/**`, `/health`, `/error`

### Protected endpoints
- All other `/api/v1/**` routes require `Authorization: Bearer <access-token>`.

### Auth principal
- JWT filter resolves token and injects `JwtAuthFilter.UserPrincipal(userId, email)`.
- Controllers consume principal via `@AuthenticationPrincipal`.

## Error Contract
- Domain/expected errors use `ApiException` and return:
  - `{ status, message, timestamp }`
- Unauthenticated requests (missing/invalid token) return:
  - `{ status: 401, message: "Authentication required", timestamp }`
  - Handled by `config/SecurityErrorHandler.java` (`AuthenticationEntryPoint`)
- Access denied (forbidden) returns:
  - `{ status: 403, message: "Access denied", timestamp }`
  - Handled by `config/SecurityErrorHandler.java` (`AccessDeniedHandler`)
- Bad login credentials return:
  - `{ status: 401, message: "Invalid email or password", timestamp }`
- Validation errors (`MethodArgumentNotValidException`) return:
  - `{ status, message: "Validation failed", errors: { field: message }, timestamp }`
- Unexpected exceptions return `500` with generic message.

## Architecture Map

### Controllers (HTTP boundary)
- `controller/AuthController.java`
- `controller/ConnectionController.java`
- `controller/AccountController.java`
- `controller/TransactionController.java`
- `controller/CategoryController.java`
- `controller/CategorizationRuleController.java`
- `controller/AnalyticsController.java`
- `controller/BudgetController.java`
- `controller/RecurringController.java`

### Core services
- Auth: `service/auth/AuthService.java`, `service/auth/JwtService.java`
- Accounts: `service/AccountService.java`
- Transactions: `service/TransactionService.java`
- Transfers: `service/TransferDetectionService.java`
- Categories: `service/CategoryService.java`
- Analytics: `service/AnalyticsService.java`
- Budgets: `service/BudgetService.java`
- Recurring facade: `service/RecurringDetectionService.java`
- SimpleFIN facade: `service/simplefin/SimpleFinSyncService.java`

### SimpleFIN decomposition
- Client: `service/simplefin/SimpleFinClient.java`
- Setup flow: `service/simplefin/SimpleFinConnectionSetupService.java`
- Sync orchestration: `service/simplefin/SimpleFinSyncOrchestrator.java`
- Quota/backfill policy: `service/simplefin/SimpleFinSyncPolicy.java`
- Shared helpers for account/transaction upserts: `service/simplefin/SimpleFinSyncSupport.java`

### Recurring decomposition
- Facade: `service/RecurringDetectionService.java`
- Detection application flow: `service/recurring/RecurringPatternApplicationService.java`
- Detection engine and thresholds: `service/recurring/RecurringPatternDetectionEngine.java`
- Query/update flows: `service/recurring/RecurringPatternQueryService.java`

### Repository split (transactions)
- Read model + filtered fetches: `repository/TransactionReadRepository.java`
- Write model + link/unlink operations: `repository/TransactionWriteRepository.java`
- Analytics projections: `repository/TransactionAnalyticsRepository.java`
- Row mapper utility: `repository/TransactionRowMappers.java`

## Endpoint To Code Path Map (Authoritative)

### Auth
- `POST /api/v1/auth/register`
  - Controller: `AuthController.register`
  - Service: `AuthService.register`
  - Repositories: `UserRepository`, `RefreshTokenRepository`
- `POST /api/v1/auth/login`
  - `AuthController.login` -> `AuthService.login`
- `POST /api/v1/auth/refresh`
  - `AuthController.refresh` -> `AuthService.refresh`
- `POST /api/v1/auth/logout`
  - `AuthController.logout` -> `AuthService.logout`
- `GET /api/v1/auth/me`
  - `AuthController.me` -> `AuthService.getCurrentUser`

### Connections / SimpleFIN
- `GET /api/v1/connections`
  - `ConnectionController.getConnections` -> `SimpleFinSyncService.getConnections`
- `POST /api/v1/connections/simplefin/setup`
  - `ConnectionController.setupSimpleFin` -> `SimpleFinSyncService.setupConnection`
  - delegates to `SimpleFinConnectionSetupService`
- `POST /api/v1/connections/{id}/sync`
  - `ConnectionController.syncConnection` -> `SimpleFinSyncService.syncConnection`
  - delegates to `SimpleFinSyncOrchestrator`
- `DELETE /api/v1/connections/{id}`
  - `ConnectionController.deleteConnection` -> `SimpleFinSyncService.deleteConnection`

### Accounts
- `GET /api/v1/accounts`
  - `AccountController.getAccounts` -> `AccountService.getAccounts`
- `GET /api/v1/accounts/summary`
  - `AccountController.getAccountSummary` -> `AccountService.getAccountSummary`
- `GET /api/v1/accounts/{id}`
  - `AccountController.getAccount` -> `AccountService.getAccount`
- `PATCH /api/v1/accounts/{id}/net-worth-category`
  - request DTO: `AccountNetWorthCategoryUpdateRequest`
  - `AccountController.updateAccountNetWorthCategory` -> `AccountService.updateNetWorthCategory`

### Transactions
- `GET /api/v1/transactions`
  - `TransactionController.getTransactions` -> `TransactionService.getTransactions` -> `TransactionReadRepository.findByUserIdWithFilters`
  - Supports optional `descriptionQuery`; matching is normalized to lower-case alphanumeric on both query and transaction description (ignores punctuation/special characters).
- `POST /api/v1/transactions`
  - request DTO: `TransactionCreateRequest`
  - `TransactionController.createTransaction` -> `TransactionService.createTransaction` -> `TransactionWriteRepository.save`
  - Validates account ownership and optional category visibility for the authenticated user.
  - If `categoryId` is omitted, auto-categorization rules are evaluated before save; if provided, transaction is marked manually categorized.
- `GET /api/v1/transactions/coverage`
  - `TransactionController.getTransactionCoverage` -> `TransactionService.getTransactionCoverage` -> `TransactionReadRepository.getCoverageByUserId`
- `GET /api/v1/transactions/{id}`
  - `TransactionController.getTransaction` -> `TransactionService.getTransaction`
- `PATCH /api/v1/transactions/{id}`
  - `TransactionController.updateTransaction` -> `TransactionService.updateTransaction` -> `TransactionWriteRepository.save`
- `DELETE /api/v1/transactions/{id}`
  - `TransactionController.deleteTransaction` -> `TransactionService.deleteTransaction` -> `TransactionWriteRepository.deleteById`
  - Only manual-entry transactions are deletable (`external_id IS NULL`); imported transactions return `400`.
  - If transaction belongs to a transfer pair, backend unlinks the pair before delete.
- `GET /api/v1/transactions/transfers`
  - `TransactionController.getTransfers` -> `TransactionService.getTransfers` -> `TransferDetectionService.getTransferPairs`
- `POST /api/v1/transactions/{id}/mark-as-transfer`
  - DTO request: `MarkTransferRequest`
  - `TransactionController.markAsTransfer` -> `TransactionService.markAsTransfer` -> `TransferDetectionService.markAsTransfer`
- `POST /api/v1/transactions/{id}/unlink-transfer`
  - response DTO: `MessageResponse`
  - `TransactionController.unlinkTransfer` -> `TransactionService.unlinkTransfer` -> `TransferDetectionService.unlinkTransfer`

### Categories
- `GET /api/v1/categories?flat=<bool>`
  - `CategoryController.getCategories` -> `CategoryService.getCategoriesForUser|getCategoriesFlatForUser`
- `GET /api/v1/categories/{id}`
  - `CategoryController.getCategory` -> `CategoryService.getCategoryById`
- `POST /api/v1/categories`
  - request DTO: `CategoryCreateRequest`
  - `CategoryController.createCategory` -> `CategoryService.createCategory`
- `PUT /api/v1/categories/{id}`
  - `CategoryController.updateCategory` -> `CategoryService.updateCategory`
- `DELETE /api/v1/categories/{id}`
  - `CategoryController.deleteCategory` -> `CategoryService.deleteCategory`
  - Clears category links from matching user transactions/recurring patterns/rules for the deleted category tree before delete/hide.

### Categorization Rules
- `GET /api/v1/categorization-rules`
  - `CategorizationRuleController.getRules` -> `CategorizationRuleService.getRulesForUser`
- `POST /api/v1/categorization-rules`
  - request DTO: `CategorizationRuleUpsertRequest`
  - `CategorizationRuleController.createRule` -> `CategorizationRuleService.createRule`
  - Rule payload supports chained `conditions[]` plus `conditionOperator` (`AND`/`OR`).
  - Condition fields now include text fields plus `ACCOUNT` and `AMOUNT`.
  - On create, backend immediately triggers `TransactionService.backfillCategorizationRules` so existing eligible transactions are re-evaluated.
- `PUT /api/v1/categorization-rules/{id}`
  - request DTO: `CategorizationRuleUpsertRequest`
  - `CategorizationRuleController.updateRule` -> `CategorizationRuleService.updateRule`
  - On update, backend immediately triggers `TransactionService.backfillCategorizationRules` so existing eligible transactions are re-evaluated.
- `DELETE /api/v1/categorization-rules/{id}`
  - `CategorizationRuleController.deleteRule` -> `CategorizationRuleService.deleteRule`
- `GET /api/v1/categorization-rules/{id}/transactions`
  - `CategorizationRuleController.getRuleTransactions` -> `TransactionService.getTransactionsForCategorizationRule`
- `POST /api/v1/categorization-rules/backfill`
  - `CategorizationRuleController.backfillRuleAssignments` -> `TransactionService.backfillCategorizationRules`
  - Re-evaluates all non-manually-categorized user transactions against active rules and persists `categorized_by_rule_id`.

### Analytics
- `GET /api/v1/analytics/spending`
  - `AnalyticsController.getSpendingByCategory` -> `AnalyticsService.getSpendingByCategory` -> `TransactionAnalyticsRepository.sumByCategory`
- `GET /api/v1/analytics/trends`
  - `AnalyticsController.getTrends` -> `AnalyticsService.getTrends` -> `TransactionAnalyticsRepository.sumByUserIdAndDateRangeAndType`
- `GET /api/v1/analytics/cashflow`
  - `AnalyticsController.getCashFlow` -> `AnalyticsService.getCashFlow` -> `TransactionAnalyticsRepository.sumByUserIdAndDateRangeAndType`

### Budgets
- `GET /api/v1/budgets?month=YYYY-MM`
  - `BudgetController.getBudgetMonth` -> `BudgetService.getBudgetMonth` -> `BudgetTargetRepository.findByUserIdAndMonthKey`
- `PUT /api/v1/budgets/{month}`
  - request DTO: `BudgetMonthUpsertRequest`
  - `BudgetController.upsertBudgetMonth` -> `BudgetService.upsertBudgetMonth` -> `BudgetTargetRepository.replaceMonthTargets`
- `DELETE /api/v1/budgets/{month}/categories/{categoryId}`
  - `BudgetController.deleteBudgetTarget` -> `BudgetService.deleteTarget` -> `BudgetTargetRepository.deleteByUserIdAndMonthKeyAndCategoryId`

### Recurring
- `GET /api/v1/recurring`
  - `RecurringController.getRecurringPatterns` -> `RecurringDetectionService.getRecurringPatterns` -> `RecurringPatternQueryService.getRecurringPatterns`
- `POST /api/v1/recurring/detect`
  - response DTO: `RecurringDetectionResponse`
  - `RecurringController.detectPatterns` -> `RecurringDetectionService.detectRecurringPatterns` -> `RecurringPatternApplicationService.detectRecurringPatterns`
- `GET /api/v1/recurring/upcoming`
  - `RecurringController.getUpcomingBills` -> query service
- `GET /api/v1/recurring/calendar`
  - `RecurringController.getBillsForMonth` -> query service
- `PATCH /api/v1/recurring/{id}`
  - request DTO: `ToggleRecurringActiveRequest`
  - `RecurringController.togglePatternActive` -> query service
- `DELETE /api/v1/recurring/{id}`
  - `RecurringController.deletePattern` -> query service

## Key Domain Workflows

### Authentication lifecycle
1. Register/login validates credentials and returns token pair.
2. Refresh token is hashed (`SHA-256`) before DB storage.
3. Refresh endpoint revokes existing refresh token and issues a new pair.
4. Logout revokes specific refresh token if provided, otherwise all for user.

### Categorization rule tracking backfill lifecycle
1. `categorized_by_rule_id` is stored when auto-categorization matches a rule.
2. Rule matching supports multi-condition `AND`/`OR` logic, including account-id and numeric amount conditions.
3. Auto-categorization ignores rules whose target category is hidden for that user.
4. Creating or updating a rule immediately backfills existing non-manually-categorized user transactions.
5. On local/Docker startup, app can backfill existing transactions per user (`app.categorization.backfill-on-startup`).
6. Manual trigger also exists at `POST /api/v1/categorization-rules/backfill`.
7. If a previously auto-categorized transaction no longer matches any active rule, backfill clears both `categorized_by_rule_id` and `category_id`.

### Manual transaction creation lifecycle
1. User submits `POST /api/v1/transactions` with account/date/amount/description and optional fields.
2. Backend validates account belongs to user and optional category is visible to that user.
3. If category is supplied, transaction is stored as manually categorized.
4. If category is omitted, backend attempts auto-categorization and stores `categorized_by_rule_id` when matched.
5. Persisted transaction is returned as the same `TransactionDto` contract used by list/detail endpoints.

### Manual transaction deletion lifecycle
1. User requests `DELETE /api/v1/transactions/{id}`.
2. Backend verifies the transaction belongs to the authenticated user.
3. Backend rejects deletion for imported entries (`external_id` present).
4. If paired as transfer, backend unlinks transfer flags from both sides first.
5. Transaction row is deleted and endpoint returns `204 No Content`.

### SimpleFIN setup lifecycle
1. Setup token is Base64 URL-decoded into claim URL.
2. Claim URL is validated to be HTTPS and under `simplefin.org` host.
3. Claim exchange returns access URL containing credentials.
4. Access URL is encrypted before storing in `simplefin_connections`.
5. Initial account pull creates/updates account records.
6. Connection metadata is saved with sync status and quota metadata.

### SimpleFIN sync/backfill lifecycle
1. Resolve connection and check request quota policy.
2. Run incremental sync window (`calculateStartDate` with overlap).
3. Upsert accounts and transactions.
   - Auto-categorized transactions persist `categorized_by_rule_id` for rule-level traceability.
   - Rules targeting hidden categories are skipped.
4. If initial history not complete, run backfill windows backward in time.
5. Backfill completion rules:
   - cutoff date reached (`1970-01-01`), or
   - 12 consecutive empty windows.
6. After transaction sync, auto-detect transfer pairs.
7. Save final sync status/result metrics on connection.

### Transfer detection logic
- Candidate window: ±5 days.
- Score components include:
  - opposite amount exact match
  - date proximity
  - transfer keyword match in descriptions/memos
  - account-type pairing heuristics
- Threshold: score >= 0.7 to auto-link.
- Linking sets transfer flags and optionally assigns system transfer category for non-manual categories.

### Recurring detection logic
- Group key: normalized transaction description.
- Minimum occurrences: 2.
- Frequency inference from average interval buckets:
  - weekly, biweekly, monthly, quarterly, yearly ranges.
- Amount variance gate:
  - reject if max variance / avg amount exceeds 0.1.
- Next expected date projected by frequency.
- Category inferred by most frequent non-null category in group.

## Data Model And Persistence

### Entities
- Auth: `User`, `RefreshToken`
- Banking: `SimpleFinConnection`, `Account`, `Transaction`
  - `Account` supports optional `net_worth_category_override` (`BANK_ACCOUNT`, `INVESTMENT`, `LIABILITY`) for dashboard grouping.
  - `Transaction` includes optional `categorized_by_rule_id` linkage when auto-categorized by a rule.
- Classification: `Category`, `CategorizationRule`
- Budgeting: `BudgetTarget`
- Recurring: `RecurringPattern`

### Migration files
- `V1__initial_schema.sql` creates all core tables.
- `V2__seed_categories.sql` seeds system categories.
- `V3__connection_initial_sync_flag.sql` adds initial sync completion flag.
- `V4__connection_backfill_cursor.sql` adds backfill cursor date.
- `V5__normalize_backfill_state.sql` normalizes conflicting backfill state.
- `V6__category_overrides.sql` adds per-user override/hide state for system categories.
- `V7__transaction_rule_tracking.sql` adds `categorized_by_rule_id` linkage on transactions.
- `V8__budget_targets.sql` adds persisted monthly category targets by user.
- `V9__account_net_worth_category_override.sql` adds optional per-account net worth category override.

## Scheduler Behavior
- Scheduler class: `scheduler/SyncScheduler.java`
- Jobs:
  - `06:30` daily: morning sync
  - `20:00` daily: evening sync
  - every 4 hours: periodic eligible sync
  - `02:00` daily: delete expired refresh tokens
  - `03:00` Sunday: recurring detection pass for due connections

## Frontend Interaction Surface (What UI Actually Uses)
- Actively used by current frontend:
  - auth endpoints
  - connections endpoints
  - accounts list + summary
  - transactions list/create/coverage/update/delete
  - categories CRUD/list
  - analytics spending/cashflow (trends endpoint exists, not currently wired to visible dashboard chart)
  - budgets month read/write/delete
- Now surfaced in frontend:
  - recurring management endpoints (list, detect, upcoming, toggle, delete)
  - transfer pair management endpoints (list, mark, unlink)
  - account detail endpoints (`GET /accounts/{id}`, `PATCH /accounts/{id}/net-worth-category`)

## Common Debug Paths
- Unauthorized/auth failures:
  1. `config/SecurityConfig.java`
  2. `config/JwtAuthFilter.java`
  3. `service/auth/AuthService.java`
  4. `repository/RefreshTokenRepository.java`
- Sync/backfill issues:
  1. `service/simplefin/SimpleFinClient.java`
  2. `service/simplefin/SimpleFinSyncPolicy.java`
  3. `service/simplefin/SimpleFinSyncOrchestrator.java`
  4. `repository/SimpleFinConnectionRepository.java`
- Transaction/transfer issues:
  1. `service/TransactionService.java`
  2. `service/TransferDetectionService.java`
  3. `repository/TransactionReadRepository.java`
  4. `repository/TransactionWriteRepository.java`
- Analytics mismatches:
  1. `service/AnalyticsService.java`
  2. `repository/TransactionAnalyticsRepository.java`
  3. `repository/CategoryRepository.java`
- Recurring issues:
  1. `service/recurring/RecurringPatternApplicationService.java`
  2. `service/recurring/RecurringPatternDetectionEngine.java`
  3. `service/recurring/RecurringPatternQueryService.java`

## Change Rules
1. Keep controller request/response payloads typed with DTO classes.
2. Keep transaction read/write/analytics concerns separated by repository.
3. Preserve endpoint paths and response field names unless intentionally versioning API.
4. When modifying service algorithms (sync/transfer/recurring), update AGENTS docs and tests in same change.

## Test Coverage
- 119 tests across 16 test classes (all passing).
- Framework: JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`.
- Coverage spans: Auth, Categories, Transactions, Analytics, Budgets, Accounts, Transfers, Recurring, CategoryView, CategorizationRules, AutoCategorization, JwtService, SecurityErrorHandler, GlobalExceptionHandler.
- Test files under `src/test/java/com/peter/budget/`:
  - `service/auth/AuthServiceTest.java` (15 tests)
  - `service/auth/JwtServiceTest.java` (11 tests)
  - `service/CategoryServiceTest.java` (13 tests)
  - `service/BudgetServiceTest.java` (11 tests)
  - `service/AccountServiceTest.java` (13 tests)
  - `service/TransferDetectionServiceTest.java` (10 tests)
  - `service/AnalyticsServiceTest.java` (9 tests)
  - `service/TransactionServiceTest.java` (7 tests)
  - `service/CategoryViewServiceTest.java` (7 tests)
  - `service/RecurringDetectionServiceTest.java` (6 tests)
  - `service/CategorizationRuleServiceTest.java` (2 tests)
  - `service/AutoCategorizationServiceTest.java` (2 tests)
  - `config/SecurityErrorHandlerTest.java` (2 tests)
  - `exception/GlobalExceptionHandlerTest.java` (8 tests)
  - `model/dto/TransactionUpdateRequestTest.java` (2 tests)
  - `BudgetApplicationTests.java` (1 test)

## Known Functional Gaps
- No integration tests (all tests are unit tests with mocked dependencies).
- No controller-level `@WebMvcTest` tests (requests are tested indirectly through service layer).
- SimpleFIN sync/backfill logic is not unit-tested (depends on external HTTP client).

## Notes Protocol (Required)
When the user supplies a correction that resolves a real issue:
1. Add short evergreen guidance under `Learned Fixes` in this file.
2. Add detailed note to `notes_to_agent/<YYYY-MM-DD>-<slug>.md`.
3. Update `notes_to_agent/index.md` in the same change.
4. Keep notes index sorted newest-first.

Use `notes_to_agent/_note_template.md` for note structure.

## Learned Fixes
- Transfer-linking flows must preserve `transfer_pair_id`, `is_internal_transfer`, and `exclude_from_totals` when applying category updates; otherwise analytics will overcount transfers as income/expense.
- Rule-backfill unmatch handling must clear both `categorized_by_rule_id` and `category_id`; clearing only rule tracking leaves stale categories that look like rule updates require restart.
