# Destiny Investment Club

A savings, loans and double-entry accounting system for an investment club, built with
Java, Spring Boot, Maven, Thymeleaf and MySQL.

## Features

- **Clients & Groups** — register members individually or as part of a savings/lending group.
- **Savings products & accounts** — define products (interest rate, minimum balance) and open
  individual or group savings accounts against them.
- **Loan products & accounts** — define loan products, apply for a loan, approve/reject it,
  disburse it (which generates a declining-balance, equal-installment repayment schedule) and
  track repayments.
- **Regular Deposit screen** (`/deposits/new`) — the core teller screen: capture one total
  amount received from a member or group, then split it across one or more savings deposits
  and/or loan repayments. The split must add up exactly to the total before it can be saved.
  The whole receipt is posted as a single balanced journal entry.
- **Double-entry accounting** — every transaction (savings deposit/withdrawal, loan
  disbursement/repayment, the combined deposit screen, and manual journal entries) posts a
  balanced journal entry to a proper chart of accounts. Reports:
  - Trial Balance
  - Balance Sheet
  - Income & Expenditure Statement
  - General Ledger (per account, with opening/closing balances)
- **Users & roles** — ADMIN, MANAGER, ACCOUNTANT and TELLER roles with different access levels,
  managed under `/users` (ADMIN only).

## Tech stack

- Java 17, Spring Boot 3.3 (Web MVC, Data JPA, Security, Validation)
- Thymeleaf + Bootstrap 5 (server-rendered UI, session-based login)
- MySQL 8 (production), Flyway (schema migrations), H2 (in-memory, test-only)
- Maven

## Prerequisites

- JDK 17+
- Maven 3.9+
- MySQL 8 running locally (or reachable) — the app will auto-create the database schema
  (`createDatabaseIfNotExist=true`), but the MySQL server itself must already be running.

## Configuration

Database and default admin credentials are configured in
`src/main/resources/application.yml`, all overridable via environment variables:

| Variable         | Default                    | Purpose                          |
|------------------|-----------------------------|-----------------------------------|
| `DB_HOST`        | `localhost`                | MySQL host                       |
| `DB_PORT`        | `3306`                     | MySQL port                       |
| `DB_NAME`        | `destinyjava`               | Database name (auto-created)     |
| `DB_USER`        | `root`                     | MySQL username                   |
| `DB_PASSWORD`    | `root`                     | MySQL password                   |

On first start-up, if there are no users in the database yet, a default administrator is
created automatically (see `app.default-admin.*` in `application.yml`, default username
`admin` / password `ChangeMe123!`). **Log in and change this password immediately** via
Users → Reset Password.

## Running

```bash
mvn spring-boot:run
```

Then open http://localhost:8080 and log in with the default admin account above.

## Running the tests

The test suite runs the whole application (Flyway migrations, JPA schema validation, a full
client → savings account → loan disbursement → combined deposit → balanced reports flow)
against an in-memory H2 database in MySQL-compatibility mode, so it needs no MySQL server:

```bash
mvn test
```

## Chart of accounts

Seeded automatically by Flyway (`V3__seed_chart_of_accounts.sql`). System (control) accounts
that the application posts to automatically:

| Code | Account               | Type      |
|------|------------------------|-----------|
| 1000 | Cash and Bank          | Asset     |
| 1100 | Loans Receivable       | Asset     |
| 2000 | Member Savings         | Liability |
| 4000 | Loan Interest Income   | Income    |

Additional accounts (share capital, retained earnings, fees income, interest/admin expense,
loan loss provision) are seeded as normal accounts you can post to via manual journal entries
(Accounting → Manual Journal Entry) — e.g. for recording rent, salaries or other running costs
so they show up on the Income & Expenditure statement. More accounts can be added at any time
under Accounting → Chart of Accounts.

## How the deposit screen keeps the books balanced

Each split line on the deposit screen becomes a savings deposit or a loan repayment against the
selected account, and the whole receipt is posted as **one** journal entry:

- Debit Cash/Bank for the total amount received
- Credit Member Savings for the savings portion
- Credit Loans Receivable for the loan principal portion
- Credit Loan Interest Income for the loan interest portion

Because the split is validated to equal the total before saving, and loan repayments are
allocated across principal and interest exactly (interest first, then principal, per
installment), the journal entry always balances automatically.

## Deploying to Railway

The app reads its HTTP port from `$PORT` (Railway sets this automatically) and its database
connection from the `DB_*` environment variables described above, so deployment is:

1. On [railway.app](https://railway.app), create a new project and choose **Deploy from GitHub
   repo**, selecting `andrewmugaaju/Destinyinvestmentclub2`. Railway's Nixpacks builder detects
   the `pom.xml` automatically, runs the Maven build, and uses the included `Procfile`
   (`web: java -jar target/investment-club.jar`) to start it.
2. Add a **MySQL** plugin to the same project (New → Database → MySQL).
3. On the app service, add these environment variables, referencing the MySQL plugin's own
   variables (Railway's `${{ServiceName.VAR}}` syntax, autocompleted in its dashboard):
   - `DB_HOST` = `${{MySQL.MYSQLHOST}}`
   - `DB_PORT` = `${{MySQL.MYSQLPORT}}`
   - `DB_NAME` = `${{MySQL.MYSQLDATABASE}}`
   - `DB_USER` = `${{MySQL.MYSQLUSER}}`
   - `DB_PASSWORD` = `${{MySQL.MYSQLPASSWORD}}`
4. Optionally set `APP_DEFAULT_ADMIN_PASSWORD`-style overrides for `app.default-admin.*` (see
   `application.yml`) so the seeded admin account doesn't use the default password in production.
5. Deploy. On first boot, Flyway creates the schema and the default admin user is seeded
   automatically — log in and change the password immediately.
