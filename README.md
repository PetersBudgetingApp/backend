# Budget Backend (SimpleFIN + Actual-style budgeting)

Spring Boot backend for a budgeting app that ingests accounts/transactions from SimpleFIN, stores them locally, and exposes authenticated APIs for budgeting workflows (categorization, analytics, transfers, recurring bills).

## Stack

- Java 21
- Spring Boot 3.5 (`web`, `security`, `jdbc`, `validation`, `webflux`)
- PostgreSQL (prod) / H2 in-memory (dev)
- Flyway migrations
- JWT auth (access + refresh tokens)
- Jasypt AES encryption for stored SimpleFIN access URLs

## How the backend works

1. User registers/logs in and receives JWT tokens.
2. User submits a SimpleFIN setup token (`/api/v1/connections/simplefin/setup`).
3. Backend exchanges setup token for SimpleFIN access URL, encrypts/stores it, and creates account records.
4. Sync (`/api/v1/connections/{id}/sync`) pulls transactions, upserts data, applies auto-categorization, and attempts transfer detection.
5. Analytics, recurring detection, category management, and transaction updates operate on the local DB.

## Local development

For the fastest full-stack launch (frontend + backend + Postgres), run from the repository root:

```bash
docker compose up --build -d
```

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (for PostgreSQL option)

### Backend-only run (H2, quickest)

Run from `backend/`:

```bash
mvn spring-boot:run
```

Defaults from `application.properties`:

- Port: `8080`
- DB: H2 in-memory (`jdbc:h2:mem:budgetdb`)
- Flyway migrations enabled

### Local PostgreSQL (recommended)

Run from `backend/` to use `backend/docker-compose.yml`.

Start PostgreSQL:

```bash
docker compose up -d
```

Run backend with local profile:

```bash
SPRING_PROFILES_ACTIVE=local \
JWT_SECRET=replace-with-a-long-local-secret \
ENCRYPTION_SECRET=replace-with-a-long-local-secret \
mvn spring-boot:run
```

Defaults in `application-local.properties`:

- DB URL: `jdbc:postgresql://localhost:5432/budget`
- DB user/password: `budget` / `budget`
- Flyway migrations enabled

Helpful commands:

```bash
docker compose ps
docker compose logs -f postgres
docker compose down
```

### Production config

Use profile `prod` and set:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `ENCRYPTION_SECRET`
- `APP_CORS_ALLOWED_ORIGINS` (comma-separated origins)

Example:

```bash
SPRING_PROFILES_ACTIVE=prod \
DATABASE_URL=jdbc:postgresql://localhost:5432/budget \
DATABASE_USERNAME=budget \
DATABASE_PASSWORD=secret \
JWT_SECRET=replace-with-strong-secret \
ENCRYPTION_SECRET=replace-with-strong-secret \
APP_CORS_ALLOWED_ORIGINS=https://your-frontend.example.com \
mvn spring-boot:run
```

## Demo / example user

A pre-populated demo account is created automatically on first startup via Flyway migration. It contains 12 months of realistic financial data (March 2025 – February 2026) so testers can explore all app features immediately.

| | |
|---|---|
| **Email** | `example.user@test.com` |
| **Password** | `example-user-password1` |

What's included:

- **6 accounts** – checking, savings, credit card, auto loan, brokerage, vacation fund (covers all account types and net-worth categories)
- **~420 transactions** – salary, rent, groceries, gas, streaming, restaurants, Amazon, travel, transfers, and more
- **3 custom categories** – Side Hustle, Loan Payments, Date Night
- **8 custom categorization rules** – Trader Joe's, Olive Garden, Petco, State Farm, Blue Cross, Oakwood Rent, Capital One Auto, City Electric
- **6 months of budget targets** – across groceries, restaurants, coffee, gas, clothing, streaming, and more
- **13 recurring patterns** – salary, rent, Netflix, Spotify, Disney+, Verizon, Comcast, Planet Fitness, iCloud, health/car insurance, car loan
- **24 linked transfer pairs** – checking → savings and checking → credit card payments

Every self-hosted user can log in with these credentials to see a fully populated dashboard, transaction history, budget insights, and recurring bills.

## Auth model

- Access token TTL: 15 minutes (`jwt.access-token-expiration-ms`)
- Refresh token TTL: 7 days (`jwt.refresh-token-expiration-ms`)
- Send access token as:

```http
Authorization: Bearer <access_token>
```

Public endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

All other `/api/v1/**` endpoints require auth.

## Scheduled jobs

- `06:30` daily: full sync attempt (`morningSyncAll`)
- `20:00` daily: full sync attempt (`eveningSyncAll`)
- Every 4 hours: periodic eligible sync (`periodicSync`)
- `02:00` daily: delete expired refresh tokens
- `03:00` Sunday: recurring pattern detection

## Error responses

All error responses use a consistent JSON shape. This includes application errors, authentication/authorization failures, and validation errors.

`ApiException` shape (application errors):

```json
{
  "status": 400,
  "message": "Error message",
  "timestamp": "2026-02-06T20:00:00Z"
}
```

Unauthenticated requests (missing or invalid token):

```json
{
  "status": 401,
  "message": "Authentication required",
  "timestamp": "2026-02-06T20:00:00Z"
}
```

Access denied (valid token but insufficient permissions):

```json
{
  "status": 403,
  "message": "Access denied",
  "timestamp": "2026-02-06T20:00:00Z"
}
```

Invalid credentials (`POST /login` with wrong password):

```json
{
  "status": 401,
  "message": "Invalid email or password",
  "timestamp": "2026-02-06T20:00:00Z"
}
```

Validation failures (`MethodArgumentNotValidException`):

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "fieldName": "reason"
  },
  "timestamp": "2026-02-06T20:00:00Z"
}
```

## API reference

### Auth

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | No | Register user and return access/refresh tokens |
| POST | `/api/v1/auth/login` | No | Login and return access/refresh tokens |
| POST | `/api/v1/auth/refresh` | No | Exchange refresh token for new token pair |
| POST | `/api/v1/auth/logout` | Yes | Revoke provided refresh token, or all user refresh tokens |
| GET | `/api/v1/auth/me` | Yes | Return current authenticated user |

Request bodies:

- `POST /register`, `POST /login`
```json
{ "email": "user@example.com", "password": "min-8-chars" }
```
- `POST /refresh`, optional for `/logout`
```json
{ "refreshToken": "<token>" }
```

### Connections (SimpleFIN)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/connections` | Yes | List user's external connections |
| POST | `/api/v1/connections/simplefin/setup` | Yes | Exchange setup token, create encrypted connection + accounts |
| POST | `/api/v1/connections/{id}/sync` | Yes | Sync one connection's accounts/transactions |
| DELETE | `/api/v1/connections/{id}` | Yes | Delete connection and all accounts/transactions tied to it |

Setup body:

```json
{ "setupToken": "<simplefin_setup_token>" }
```

### Accounts

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/accounts` | Yes | List active accounts |
| GET | `/api/v1/accounts/summary` | Yes | Assets, liabilities, net worth, and account list |
| GET | `/api/v1/accounts/{id}` | Yes | Get one account |

### Transactions

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/transactions` | Yes | List transactions with filters/pagination |
| GET | `/api/v1/transactions/{id}` | Yes | Get one transaction |
| PATCH | `/api/v1/transactions/{id}` | Yes | Update category/notes/excludeFromTotals |
| GET | `/api/v1/transactions/transfers` | Yes | List detected transfer pairs |
| POST | `/api/v1/transactions/{id}/mark-as-transfer` | Yes | Link two transactions as transfer pair |
| POST | `/api/v1/transactions/{id}/unlink-transfer` | Yes | Remove transfer link |

`GET /api/v1/transactions` query params:

- `includeTransfers` (default `false`)
- `startDate` (`YYYY-MM-DD`)
- `endDate` (`YYYY-MM-DD`)
- `categoryId`
- `accountId`
- `limit` (default `100`)
- `offset` (default `0`)

`PATCH /api/v1/transactions/{id}` body (any subset):

```json
{
  "categoryId": 123,
  "notes": "Optional note",
  "excludeFromTotals": false
}
```

`POST /api/v1/transactions/{id}/mark-as-transfer` body:

```json
{ "pairTransactionId": 456 }
```

### Categories

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/categories` | Yes | List categories (tree by default) |
| GET | `/api/v1/categories/{id}` | Yes | Get one category |
| POST | `/api/v1/categories` | Yes | Create custom category |
| PUT | `/api/v1/categories/{id}` | Yes | Update custom category |
| DELETE | `/api/v1/categories/{id}` | Yes | Delete custom category |

`GET /api/v1/categories` query params:

- `flat` (default `false`; set `true` for flat list)

Create/update body:

```json
{
  "parentId": null,
  "name": "Dining",
  "icon": "utensils",
  "color": "#F59E0B",
  "categoryType": "EXPENSE"
}
```

### Analytics

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/analytics/spending` | Yes | Spending grouped by category |
| GET | `/api/v1/analytics/trends` | Yes | Monthly trend series |
| GET | `/api/v1/analytics/cashflow` | Yes | Income/expense/transfers/savings rate |

Params:

- `/spending`: `startDate`, `endDate` (optional)
- `/trends`: `months` (default `6`)
- `/cashflow`: `startDate`, `endDate` (optional)

### Budgets

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/budgets?month=YYYY-MM` | Yes | Get monthly budget targets for the authenticated user |
| PUT | `/api/v1/budgets/{month}` | Yes | Replace monthly category targets for the authenticated user |
| DELETE | `/api/v1/budgets/{month}/categories/{categoryId}` | Yes | Delete one category target for a month |

`PUT /api/v1/budgets/{month}` body:

```json
{
  "targets": [
    {
      "categoryId": 123,
      "targetAmount": 500.0,
      "notes": "Groceries + dining"
    }
  ]
}
```

### Recurring

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/recurring` | Yes | List active recurring patterns |
| POST | `/api/v1/recurring/detect` | Yes | Trigger recurring pattern detection |
| GET | `/api/v1/recurring/upcoming` | Yes | Upcoming bills in next `days` |
| GET | `/api/v1/recurring/calendar` | Yes | Bills for given year/month |
| PATCH | `/api/v1/recurring/{id}` | Yes | Toggle pattern active flag |
| DELETE | `/api/v1/recurring/{id}` | Yes | Delete recurring pattern |

Params/body:

- `/upcoming`: `days` (default `30`)
- `/calendar`: `year`, `month`
- `PATCH /api/v1/recurring/{id}`:

```json
{ "active": true }
```
