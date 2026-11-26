# WalletQ Architecture Specification

**Application:** WalletQ  
**Developer:** SuBZeRo  
**Package:** `com.walletq`  
**License:** Apache License 2.0  
**Current Phase:** 3B — Account Integrity & Reconciliation  
**Database Schema Version:** 4  

---

## 1. Architectural Philosophy & Principles

WalletQ is engineered as a privacy-first, offline-first personal and household financial management platform. 

The software architecture is governed by five core tenets:
1. **Uncompromising Privacy:** Financial data is personal and sensitive. WalletQ requires zero accounts, zero phone numbers, and has zero analytics or tracking SDKs. It does not declare or use the `android.permission.INTERNET` permission.
2. **Single Source of Truth (SSOT):** The local SQLite/Room database (`walletq.db`) is the sole authoritative state of truth on the device.
3. **Clean Architecture & Unidirectional Data Flow (UDF):**
   - **Presentation Layer (Jetpack Compose):** Consumes immutable UI state streams and dispatches user intentions as events. Contains zero financial calculation or database access logic.
   - **Domain Layer (Kotlin):** Pure business entities (`Transaction`, `LedgerEntry`, `TransferDetails`, `Category`, `Money`, `Currency`, `OwnershipType`, `HouseholdRole`, `Household`, `HouseholdMember`, `Account`, `Profile`, `ExchangeRate`, `LedgerEngine`) completely isolated from Android SDK dependencies.
   - **Data Layer (Room + DataStore):** Concrete implementations of persistence, schema definitions, DAOs, indices, atomic transaction runner, and preference management.
   - **Repository Layer:** Mediates between Domain and Data layers, coordinating `LedgerEngine` domain validation, double-entry ledger persistence, balance projection, and atomic transaction boundaries.
4. **Deterministic Financial Math:** Monetary amounts are stored as exact 64-bit signed integers representing minor units, entirely avoiding binary floating-point round-off errors.
5. **Immutable Event Ledger & Auditability:** Transactions generate immutable atomic ledger entries. Financial corrections use compensating reversals rather than destructive updates. Financial records use `RESTRICT` foreign keys preventing accidental cascade deletion.

---

## 2. Layer Diagram

```
+-------------------------------------------------------------+
|                     Presentation Layer                      |
|       Jetpack Compose Material 3 UI (Screens & Components)   |
|            MainViewModel (StateFlow, UI State)              |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                      Repository Layer                       |
|   TransactionRepository (Atomic Writes, Balance Projection) |
|   CategoryRepository (Ownership, Archival & Delete Guards)  |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                        Domain Layer                         |
|     Financial Ledger Engine (LedgerEngine, Transaction,      |
|    LedgerEntry, TransferDetails, Reversal/Replacement,      |
|     Category, Money, Currency, Household, HouseholdMember)  |
+------------------------------+------------------------------+
                               ^
                               |
+-------------------------------------------------------------+
|                         Data Layer                          |
| Room Database (Version 3), TransactionRunner, DAOs:         |
| (TransactionDao, LedgerDao, CategoryDao, AccountDao,        |
|  ProfileDao, HouseholdDao, CurrencyDao, ExchangeRateDao)    |
| DataStore Preferences                                       |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                          Core Layer                         |
|      Design System (M3 Tokens, Theme, Colors, Typography)   |
|      Navigation (5 Top-Level Destinations, NavHost)         |
|      Dependency Injection (Hilt Singleton Component)        |
+------------------------------+------------------------------+
```

---

## 3. Financial Ledger Domain Architecture (Phase 2A)

The core domain layer encapsulates all double-entry and ledger logic in pure Kotlin:

- **`Transaction`**: Immutable business event capturing income, expenses, or transfers with positive magnitude `amount: Money`, ownership scope, and lifecycle status (`POSTED`, `REVERSED`).
- **`LedgerEntry`**: Atomic credit/debit record affecting a single account with signed minor units (`+` for CREDIT, `-` for DEBIT).
- **`TransferDetails`**: Encapsulates destination account, destination amount, applied exchange rate, and rate reference for both same-currency and cross-currency transfers.
- **`Category`**: Domain classification model for transactions, supporting predefined system categories as well as personal and household user-defined categories.
- **`LedgerEngine`**: Pure domain service responsible for:
  - Transaction semantic validation.
  - Generating double-entry ledger entries.
  - Deriving account balances with overflow prevention (`Math.addExact`).
  - Generating transaction reversals and replacements while preserving audit trails.
  - Enforcing personal vs. household boundary isolation.

---

## 4. Room Database & Repository Architecture (Phase 2B)

The persistence and repository layer connects the pure domain engine to Room:

1. **Room Entities:**
   - `TransactionEntity`: Maps `Transaction` domain model into `transactions` table with full transfer details, audit IDs, and strict foreign keys.
   - `LedgerEntryEntity`: Maps `LedgerEntry` domain model into `ledger_entries` table with signed minor units and composite indices `(accountId, timestampEpochMs)`.
   - `CategoryEntity`: Maps `Category` domain model into `categories` table with system category flags and hierarchical self-references.

2. **Atomic Transaction Runner (`TransactionRunner`):**
   - Coordinates multi-table atomic writes (`transactions`, `ledger_entries`, `accounts`) inside `database.withTransaction`.
   - Guarantees that no transaction is ever persisted without its accompanying ledger entries.
   - Guarantees immediate rollback on validation failure, constraint violation, or arithmetic overflow.

3. **Repository Implementations:**
   - `TransactionRepositoryImpl`: Validates business rules via `LedgerEngine`, persists transactions and ledger entries, updates account balances atomically, and handles reversals and replacements.
   - `CategoryRepositoryImpl`: Validates category ownership boundaries (system vs personal vs household), handles archival, and prevents physical deletion of categories referenced by posted transactions.

4. **Schema Migrations:**
   - `MIGRATION_1_2`: Introduced `households`, `household_members`, `exchange_rates`, and reconstructed `accounts`.
   - `MIGRATION_2_3`: Introduces `categories`, `transactions`, `ledger_entries`, creates indices, and pre-populates default system categories idempotently without altering existing financial data.

---

## 5. Transaction Management Presentation Layer (Phase 2C)

The presentation layer connects `TransactionRepository`, `CategoryRepository`, and Room DAOs to Material 3 Jetpack Compose interfaces:

1. **`TransactionViewModel` (`@HiltViewModel`):**
   - Combines reactive `Flow` streams from `TransactionRepository`, `CategoryRepository`, `AccountDao`, `ProfileDao`, and `HouseholdDao` into an immutable `StateFlow<ActivityUiState>`.
   - Manages `TransactionFormState` (Income/Expense) and `TransferFormState` (Same-Currency and Cross-Currency).
   - Uses `MoneyInputParser` (`BigDecimal`) to validate currency-specific decimal precision without `Float` or `Double`.
   - Enforces duplicate submission prevention (`isSubmitting`) and coordinates reversals via `TransactionRepository.reverseTransaction`.

2. **UI Screens & Sheets (`com.walletq.feature.activity`):**
   - `ActivityScreen` & `ActivityScreenContent`: Chronological date-grouped transaction list with search, ownership, type, account, and date-range filters, plus Loading, Empty, and Error states.
   - `AddTransactionSheet`: Income/Expense entry sheet with ownership context switching, account currency display, category picker, and Material 3 date picker.
   - `TransferSheet`: Dedicated transfer interface with same-account prevention, prefilled/manual exchange rate support, and live Banker-rounded conversion preview.
   - `TransactionDetailsSheet`: Full audit view showing transfer metadata, original/reversal references, double-entry ledger entries, and explicit reversal confirmation dialog.

---

## 6. Account Management Foundation (Phase 3A)

1. **Schema & Domain Extensions (`MIGRATION_3_4`, Version 4):**
   - `AccountEntity` and `Account` include `openingBalanceDateEpochMs`, optional `description`, `iconIdentifier`, and `colorIdentifier`, with non-destructive `ALTER TABLE` migration from Version 3.
   - `AccountType` exposes stable enum identifiers (`CASH`, `BANK`, `SAVINGS`, `CREDIT_CARD`, `INVESTMENT`, `OTHER`) with display labels.

2. **Repository & Ledger Integrity (`AccountRepository`, `AccountRepositoryImpl`):**
   - Enforces ownership boundaries (Personal vs Household membership verification) and duplicate account name prevention within each ownership scope.
   - Preserves `initialBalanceMinorUnits` and `openingBalanceDateEpochMs` on descriptive edits (`updateAccountDetails`).
   - Provides auditable balance corrections (`adjustBalance`) that record `"Opening balance adjustment"` transactions and ledger entries instead of mutating historical records.
   - Prevents physical deletion of accounts with financial history (`deleteAccount`), enforces ledger-projection consistency before archiving (`archiveAccount`), and excludes archived accounts from new transaction selectors while keeping them visible in historical transaction views.

3. **Presentation Layer (`com.walletq.feature.accounts`):**
   - `AccountViewModel` & `AccountsUiState`: Groups active balances strictly by currency (never summing unlike currencies), supports search, ownership, type, and archived filters.
   - `AccountsScreen`, `AddEditAccountSheet`, `AccountDetailsSheet`, `AdjustBalanceSheet`: Material 3 account management experience with account-specific transaction history and archive confirmation.

---

## 7. Account Integrity & Reconciliation (Phase 3B)

- **`AccountReconciliationEngine` (`com.walletq.core.model`):** Pure Kotlin read-only auditor that compares `InitialBalance + LedgerDelta` against stored `currentBalanceMinorUnits` and detects missing/duplicate ledger entries, currency mismatches, inconsistent counts, and incomplete reversals without silently mutating data.
- **`AccountDetailsSheet` Reconciliation Card:** Displays Expected Balance, Stored Balance, Difference, Reconciliation Status (`Balanced` / `Discrepancy Found`), and discrepancy explanations, plus Credit Card Outstanding Liability clarity.
