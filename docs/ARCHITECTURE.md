# WalletQ Architecture Specification

**Application:** WalletQ  
**Developer:** SuBZeRo  
**Package:** `com.walletq`  
**License:** Apache License 2.0  
**Phase:** 2A — Financial Ledger Domain Engine  

---

## 1. Architectural Philosophy & Principles

WalletQ is engineered as a privacy-first, offline-first personal and household financial management platform. 

The software architecture is governed by five core tenets:
1. **Uncompromising Privacy:** Financial data is personal and sensitive. WalletQ requires zero accounts, zero phone numbers, and has zero analytics or tracking SDKs. It does not declare or use the `android.permission.INTERNET` permission.
2. **Single Source of Truth (SSOT):** The local SQLite/Room database (`walletq.db`) is the sole authoritative state of truth on the device.
3. **Clean Architecture & Unidirectional Data Flow (UDF):**
   - **Presentation Layer (Jetpack Compose):** Consumes immutable UI state streams and dispatches user intentions as events. Contains zero financial calculation or database access logic.
   - **Domain Layer (Kotlin):** Pure business entities (`Transaction`, `LedgerEntry`, `Money`, `Currency`, `OwnershipType`, `HouseholdRole`, `Household`, `HouseholdMember`, `Account`, `Profile`, `ExchangeRate`, `LedgerEngine`) completely isolated from Android SDK dependencies.
   - **Data Layer (Room + DataStore):** Concrete implementations of persistence, schema definitions, indices, and preference management.
4. **Deterministic Financial Math:** Monetary amounts are stored as exact 64-bit signed integers representing minor units, entirely avoiding binary floating-point round-off errors.
5. **Immutable Event Ledger & Auditability:** Transactions generate immutable atomic ledger entries. Financial corrections use compensating reversals rather than destructive updates.

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
|                        Domain Layer                         |
|     Financial Ledger Engine (LedgerEngine, Transaction,      |
|    LedgerEntry, TransferDetails, Reversal/Replacement,      |
|      Money, Currency, Household, HouseholdMember)           |
+------------------------------+------------------------------+
                               ^
                               |
+-------------------------------------------------------------+
|                         Data Layer                          |
| Room DAOs & Entities (Profile, Household, Member, Account,  |
| Currency, ExchangeRate) & DataStore Preferences             |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                          Core Layer                         |
|      Design System (M3 Tokens, Theme, Colors, Typography)   |
|      Navigation (5 Top-Level Destinations, NavHost)         |
|      Dependency Injection (Hilt Singleton Component)        |
+-------------------------------------------------------------+
```

---

## 3. Financial Ledger Domain Architecture (Phase 2A)

The core domain layer encapsulates all double-entry and ledger logic in pure Kotlin:

- **`Transaction`**: Immutable business event capturing income, expenses, or transfers with positive magnitude `amount: Money`, ownership scope, and lifecycle status (`POSTED`, `REVERSED`).
- **`LedgerEntry`**: Atomic credit/debit record affecting a single account with signed minor units (`+` for CREDIT, `-` for DEBIT).
- **`TransferDetails`**: Encapsulates destination account, destination amount, applied exchange rate, and rate reference for both same-currency and cross-currency transfers.
- **`LedgerEngine`**: Pure domain service responsible for:
  - Transaction semantic validation.
  - Generating double-entry ledger entries.
  - Deriving account balances with overflow prevention (`Math.addExact`).
  - Generating transaction reversals and replacements while preserving audit trails.
  - Enforcing personal vs. household boundary isolation.
