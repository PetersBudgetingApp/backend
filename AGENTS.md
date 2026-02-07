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
- `controller/RecurringController.java`

### Core services
- Auth: `service/auth/AuthService.java`, `service/auth/JwtService.java`
- Accounts: `service/AccountService.java`
- Transactions: `service/TransactionService.java`
- Transfers: `service/TransferDetectionService.java`
- Categories: `service/CategoryService.java`
- Analytics: `service/AnalyticsService.java`
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

### Transactions
- `GET /api/v1/transactions`
  - `TransactionController.getTransactions` -> `TransactionService.getTransactions` -> `TransactionReadRepository.findByUserIdWithFilters`
- `GET /api/v1/transactions/coverage`
  - `TransactionController.getTransactionCoverage` -> `TransactionService.getTransactionCoverage` -> `TransactionReadRepository.getCoverageByUserId`
- `GET /api/v1/transactions/{id}`
  - `TransactionController.getTransaction` -> `TransactionService.getTransaction`
- `PATCH /api/v1/transactions/{id}`
  - `TransactionController.updateTransaction` -> `TransactionService.updateTransaction` -> `TransactionWriteRepository.save`
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

### Categorization Rules
- `GET /api/v1/categorization-rules`
  - `CategorizationRuleController.getRules` -> `CategorizationRuleService.getRulesForUser`
- `POST /api/v1/categorization-rules`
  - request DTO: `CategorizationRuleUpsertRequest`
  - `CategorizationRuleController.createRule` -> `CategorizationRuleService.createRule`
- `PUT /api/v1/categorization-rules/{id}`
  - request DTO: `CategorizationRuleUpsertRequest`
  - `CategorizationRuleController.updateRule` -> `CategorizationRuleService.updateRule`
- `DELETE /api/v1/categorization-rules/{id}`
  - `CategorizationRuleController.deleteRule` -> `CategorizationRuleService.deleteRule`

### Analytics
- `GET /api/v1/analytics/spending`
  - `AnalyticsController.getSpendingByCategory` -> `AnalyticsService.getSpendingByCategory` -> `TransactionAnalyticsRepository.sumByCategory`
- `GET /api/v1/analytics/trends`
  - `AnalyticsController.getTrends` -> `AnalyticsService.getTrends` -> `TransactionAnalyticsRepository.sumByUserIdAndDateRangeAndType`
- `GET /api/v1/analytics/cashflow`
  - `AnalyticsController.getCashFlow` -> `AnalyticsService.getCashFlow` -> `TransactionAnalyticsRepository.sumByUserIdAndDateRangeAndType`

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
- Classification: `Category`, `CategorizationRule`
- Recurring: `RecurringPattern`

### Migration files
- `V1__initial_schema.sql` creates all core tables.
- `V2__seed_categories.sql` seeds system categories.
- `V3__connection_initial_sync_flag.sql` adds initial sync completion flag.
- `V4__connection_backfill_cursor.sql` adds backfill cursor date.
- `V5__normalize_backfill_state.sql` normalizes conflicting backfill state.
- `V6__category_overrides.sql` adds per-user override/hide state for system categories.

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
  - transactions list/coverage/update
  - categories CRUD/list
  - analytics spending/cashflow (trends endpoint exists, not currently wired to visible dashboard chart)
- Exposed in backend but not surfaced as full frontend workflows:
  - recurring management endpoints
  - transfer pair management endpoints

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

## Known Functional Gaps
- No server-side `/budgets` endpoint group yet.
- Test suite is minimal (mostly context-load), so service-level regression tests are still needed for high-safety refactors.

## Notes Protocol (Required)
When the user supplies a correction that resolves a real issue:
1. Add short evergreen guidance under `Learned Fixes` in this file.
2. Add detailed note to `notes_to_agent/<YYYY-MM-DD>-<slug>.md`.
3. Update `notes_to_agent/index.md` in the same change.
4. Keep notes index sorted newest-first.

Use `notes_to_agent/_note_template.md` for note structure.

## Learned Fixes
- None yet.
