# WalletQ Database Schema Specification

**Application:** WalletQ  
**Lead Architect:** SuBZeRo  
**Database File:** `walletq.db`  
**Current Version:** 3  
**Export Location:** `app/schemas/com.walletq.core.database.WalletQDatabase/3.json`  

---

## 1. Architectural Principles

1. **Deterministic Integer Math:** Monetary balances and amounts are stored exclusively as 64-bit signed integers (`INTEGER` / Kotlin `Long`) representing ISO-4217 minor units. `REAL`, `FLOAT`, or `DOUBLE` columns are strictly forbidden for financial calculations.
2. **Double-Entry Ledger Foundation:** Every financial transaction creates immutable debit and credit records in `ledger_entries`. Account balances are derived from opening balances and ledger history.
3. **Audit Trail & Immutability:** Financial transaction records and ledger entries are never silently overwritten or physically deleted. Corrections use linked reversals (`reversalTransactionId`) and replacement records (`originalTransactionId`).
4. **Strict Foreign Key Deletion Policies:**
   - Account deletion policy is `RESTRICT` on financial transactions and ledger entries. Accounts with transaction history can never be cascade-deleted.
   - Category deletion policy is `RESTRICT`. Categories referenced by transactions cannot be physically deleted; they must be archived.
   - Profile/Household ownership references for custom categories use `CASCADE` to preserve clean account boundaries.
5. **Ownership Isolation:** Personal accounts and transactions are isolated by `profileId`. Household accounts and transactions are linked to `householdId` and audited against `household_members`.

---

## 2. Entity Relational Model (Version 3)

### 2.1. `profiles`
Represents an individual user identity.

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `id` | `TEXT` | No | Primary Key (e.g. `default-personal`, UUID) |
| `name` | `TEXT` | No | Profile display name |
| `type` | `TEXT` | No | Ownership type (`PERSONAL`, `HOUSEHOLD`) |
| `isDefault` | `INTEGER` | No | Boolean flag (`1` for default active profile) |
| `createdAtEpochMs` | `INTEGER` | No | Epoch millisecond timestamp |

---

### 2.2. `households`
Represents a collective household budgeting unit.

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `id` | `TEXT` | No | Primary Key (e.g. `default-household`, UUID) |
| `name` | `TEXT` | No | Household display name |
| `description` | `TEXT` | Yes | Optional descriptive note |
| `createdAtEpochMs` | `INTEGER` | No | Creation timestamp |

---

### 2.3. `household_members`
Junction table linking profiles to households with role-based access.

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `householdId` | `TEXT` | No | FK $\rightarrow$ `households(id)` ON DELETE CASCADE |
| `profileId` | `TEXT` | No | FK $\rightarrow$ `profiles(id)` ON DELETE CASCADE |
| `role` | `TEXT` | No | Role enum (`OWNER`, `ADMIN`, `MEMBER`, `VIEWER`) |
| `joinedAtEpochMs` | `INTEGER` | No | Join timestamp |

**Primary Key:** `(householdId, profileId)`  
**Indices:** `index_household_members_householdId`, `index_household_members_profileId`

---

### 2.4. `currencies`
ISO-4217 currency metadata catalogue.

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `code` | `TEXT` | No | Primary Key, ISO-4217 uppercase (e.g. `INR`, `USD`, `JPY`) |
| `displayName` | `TEXT` | No | Full currency name |
| `symbol` | `TEXT` | No | Display symbol (e.g. `₹`, `$`, `¥`) |
| `decimalPlaces` | `INTEGER` | No | Subunit precision (`2` for INR/USD, `0` for JPY) |
| `isDefaultReporting` | `INTEGER` | No | Default reporting currency indicator |

---

### 2.5. `exchange_rates`
Historical currency exchange rates.

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `id` | `TEXT` | No | Primary Key |
| `baseCurrencyCode` | `TEXT` | No | FK $\rightarrow$ `currencies(code)` ON DELETE CASCADE |
| `targetCurrencyCode` | `TEXT` | No | FK $\rightarrow$ `currencies(code)` ON DELETE CASCADE |
| `rate` | `TEXT` | No | Exact decimal multiplier stored as text (BigDecimal) |
| `timestampEpochMs` | `INTEGER` | No | Historical rate timestamp |

**Indices:** `index_exchange_rates_baseCurrencyCode`, `index_exchange_rates_targetCurrencyCode`

---

### 2.6. `accounts`
Financial accounts (Cash, Bank, Savings, Credit Card, etc.).

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `id` | `TEXT` | No | Primary Key |
| `profileId` | `TEXT` | No | FK $\rightarrow$ `profiles(id)` ON DELETE CASCADE |
| `householdId` | `TEXT` | Yes | FK $\rightarrow$ `households(id)` ON DELETE SET NULL |
| `ownershipType` | `TEXT` | No | Enum (`PERSONAL`, `HOUSEHOLD`) |
| `name` | `TEXT` | No | Account name |
| `type` | `TEXT` | No | Account type (`CASH`, `BANK`, `SAVINGS`, etc.) |
| `currencyCode` | `TEXT` | No | FK $\rightarrow$ `currencies(code)` ON DELETE RESTRICT |
| `initialBalanceMinorUnits` | `INTEGER` | No | Signed 64-bit integer opening balance |
| `currentBalanceMinorUnits` | `INTEGER` | No | Projected balance cached from ledger |
| `isArchived` | `INTEGER` | No | `1` if archived, `0` if active |
| `createdAtEpochMs` | `INTEGER` | No | Creation timestamp |

**Indices:** `index_accounts_profileId`, `index_accounts_householdId`, `index_accounts_currencyCode`

---

### 2.7. `categories` (Added in Version 3)
Income and expense classification categories.

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `id` | `TEXT` | No | Primary Key (e.g. `cat-food`, UUID) |
| `name` | `TEXT` | No | Category name |
| `transactionType` | `TEXT` | No | `INCOME` or `EXPENSE` |
| `iconIdentifier` | `TEXT` | No | Material icon name |
| `colorIdentifier` | `TEXT` | No | Hex color code |
| `parentCategoryId` | `TEXT` | Yes | FK $\rightarrow$ `categories(id)` ON DELETE RESTRICT |
| `profileId` | `TEXT` | Yes | FK $\rightarrow$ `profiles(id)` ON DELETE CASCADE |
| `householdId` | `TEXT` | Yes | FK $\rightarrow$ `households(id)` ON DELETE CASCADE |
| `isSystemCategory` | `INTEGER` | No | `1` for default system categories, `0` for custom |
| `isArchived` | `INTEGER` | No | Archival flag (`1` = archived, `0` = active) |
| `createdAtEpochMs` | `INTEGER` | No | Creation timestamp |

**Indices:**
- `index_categories_parentCategoryId`
- `index_categories_profileId`
- `index_categories_householdId`
- `index_categories_transactionType`

---

### 2.8. `transactions` (Added in Version 3)
High-level immutable financial event records.

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `id` | `TEXT` | No | Primary Key (UUID) |
| `transactionType` | `TEXT` | No | Enum (`INCOME`, `EXPENSE`, `TRANSFER`) |
| `transactionStatus` | `TEXT` | No | Enum (`POSTED`, `REVERSED`) |
| `amountMinorUnits` | `INTEGER` | No | Positive magnitude in minor units |
| `currencyCode` | `TEXT` | No | FK $\rightarrow$ `currencies(code)` ON DELETE RESTRICT |
| `sourceAccountId` | `TEXT` | No | FK $\rightarrow$ `accounts(id)` ON DELETE RESTRICT |
| `destinationAccountId` | `TEXT` | Yes | FK $\rightarrow$ `accounts(id)` ON DELETE RESTRICT |
| `categoryId` | `TEXT` | Yes | FK $\rightarrow$ `categories(id)` ON DELETE RESTRICT |
| `profileId` | `TEXT` | Yes | FK $\rightarrow$ `profiles(id)` ON DELETE RESTRICT |
| `householdId` | `TEXT` | Yes | FK $\rightarrow$ `households(id)` ON DELETE RESTRICT |
| `description` | `TEXT` | No | Event summary |
| `notes` | `TEXT` | Yes | User notes |
| `transactionDateEpochMs` | `INTEGER` | No | Date when event occurred |
| `createdAtEpochMs` | `INTEGER` | No | Record creation timestamp |
| `updatedAtEpochMs` | `INTEGER` | No | Record update/reversal timestamp |
| `originalTransactionId` | `TEXT` | Yes | FK $\rightarrow$ `transactions(id)` ON DELETE RESTRICT |
| `reversalTransactionId` | `TEXT` | Yes | Reference to compensating reversal transaction |
| `exchangeRate` | `TEXT` | Yes | Applied exchange rate text (for cross-currency transfers) |
| `exchangeRateReferenceId` | `TEXT` | Yes | Reference to `exchange_rates(id)` |
| `destinationAmountMinorUnits` | `INTEGER` | Yes | Minor units credited to destination account |
| `destinationCurrencyCode` | `TEXT` | Yes | FK $\rightarrow$ `currencies(code)` ON DELETE RESTRICT |

**Indices:**
- `index_transactions_sourceAccountId`
- `index_transactions_destinationAccountId`
- `index_transactions_currencyCode`
- `index_transactions_profileId`
- `index_transactions_householdId`
- `index_transactions_categoryId`
- `index_transactions_originalTransactionId`
- `index_transactions_destinationCurrencyCode`
- `index_transactions_transactionDateEpochMs`
- `index_transactions_transactionType`
- `index_transactions_transactionStatus`

---

### 2.9. `ledger_entries` (Added in Version 3)
Atomic double-entry bookkeeping records.

| Column | Affinity | Nullable | Constraints / Description |
|---|---|---|---|
| `id` | `TEXT` | No | Primary Key (UUID) |
| `transactionId` | `TEXT` | No | FK $\rightarrow$ `transactions(id)` ON DELETE RESTRICT |
| `accountId` | `TEXT` | No | FK $\rightarrow$ `accounts(id)` ON DELETE RESTRICT |
| `signedMinorUnits` | `INTEGER` | No | Signed change ($+ \text{CREDIT}, - \text{DEBIT}$) |
| `currencyCode` | `TEXT` | No | FK $\rightarrow$ `currencies(code)` ON DELETE RESTRICT |
| `entryType` | `TEXT` | No | Enum (`CREDIT`, `DEBIT`) |
| `timestampEpochMs` | `INTEGER` | No | Ledger entry timestamp |

**Indices:**
- `index_ledger_entries_transactionId`
- `index_ledger_entries_accountId`
- `index_ledger_entries_currencyCode`
- `index_ledger_entries_timestampEpochMs`
- `index_ledger_entries_accountId_timestampEpochMs` (Composite for running balance)

---

## 3. Migration History

### Migration 1 $\rightarrow$ 2 (`MIGRATION_1_2`)
- Created `households` table.
- Created `household_members` junction table.
- Created `exchange_rates` table.
- Migrated `accounts` table non-destructively to include `householdId`, `ownershipType`, and `RESTRICT` currency foreign key.
- Pre-populated default personal profile, primary household, and junction member.

### Migration 2 $\rightarrow$ 3 (`MIGRATION_2_3`)
- Created `categories` table with system category support and hierarchy references.
- Created `transactions` table with full double-entry metadata, transfer details, and audit references.
- Created `ledger_entries` table with signed minor units and composite indices.
- Configured strict `ON DELETE RESTRICT` foreign keys on financial history to prevent accidental data loss.
- Pre-populated standard system categories (12 expense, 5 income) idempotently.
- Preserved all existing accounts, households, profiles, currencies, and exchange rates.
