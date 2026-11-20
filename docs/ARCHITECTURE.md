# WalletQ Architecture Specification

**Application:** WalletQ  
**Developer:** SuBZeRo  
**Package:** `com.walletq`  
**License:** Apache License 2.0  
**Phase:** 1 — Project Initialization & Architecture Validation (Schema V2)  

---

## 1. Architectural Philosophy & Principles

WalletQ is engineered as a privacy-first, offline-first personal and household financial management platform. 

The software architecture is governed by four core tenets:
1. **Uncompromising Privacy:** Financial data is personal and sensitive. WalletQ requires zero accounts, zero phone numbers, and has zero analytics or tracking SDKs. It does not declare or use the `android.permission.INTERNET` permission.
2. **Single Source of Truth (SSOT):** The local SQLite/Room database (`walletq.db`) is the sole authoritative state of truth on the device.
3. **Clean Architecture & Unidirectional Data Flow (UDF):**
   - **Presentation Layer (Jetpack Compose):** Consumes immutable UI state streams and dispatches user intentions as events. Contains zero financial calculation or database access logic.
   - **Domain Layer (Kotlin):** Pure business entities (`Money`, `Currency`, `OwnershipType`, `HouseholdRole`, `Household`, `HouseholdMember`, `Account`, `Profile`, `ExchangeRate`) completely isolated from Android SDK dependencies.
   - **Data Layer (Room + DataStore):** Concrete implementations of persistence, schema definitions, indices, and preference management.
4. **Deterministic Financial Math:** Monetary amounts are stored as exact 64-bit signed integers representing minor units, entirely avoiding binary floating-point round-off errors.
5. **Zero Destructive Migrations:** Financial databases must never silently drop tables. Schema changes use explicit `Migration` pipelines (`MIGRATION_1_2`).

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
| Pure Kotlin Models (Money, Currency, Household, Member)     |
|                Contracts & Business Rules                   |
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

## 3. Database Strategy & Integrity Model (Schema V2)

The local SQLite database (`walletq.db`) is managed via AndroidX Room.

### Key Relational Safeguards:
1. **Foreign Key Enforcement:** SQLite does not enable foreign key enforcement by default. WalletQ explicitly executes `PRAGMA foreign_keys = ON;` inside the `RoomDatabase.Callback.onOpen` hook.
2. **Atomic Pre-Population:** System baseline currencies, the default personal profile, default household, and owner membership are inserted synchronously during `onCreate(db: SupportSQLiteDatabase)` within the open creation transaction, eliminating race conditions.
3. **Foreign Key Integrity:**
   - `accounts.profileId` references `profiles.id` (ON DELETE CASCADE)
   - `accounts.householdId` references `households.id` (ON DELETE SET NULL)
   - `accounts.currencyCode` references `currencies.code` (ON DELETE RESTRICT)
   - `household_members.householdId` references `households.id` (ON DELETE CASCADE)
   - `household_members.profileId` references `profiles.id` (ON DELETE CASCADE)
   - `exchange_rates.baseCurrencyCode` references `currencies.code` (ON DELETE CASCADE)
   - `exchange_rates.targetCurrencyCode` references `currencies.code` (ON DELETE CASCADE)
4. **No Destructive Migration Fallback:** The application uses explicit `Migration(1, 2)` handling table creation, account migration, and indexing.

---

## 4. Household Relational Architecture

WalletQ distinguishes between individual profiles and collective household boundaries without conflating them:

1. **`ProfileEntity`**: Represents an individual user persona on the device.
2. **`HouseholdEntity`**: Represents an administrative financial unit (e.g. "Family Budget", "Flat 402").
3. **`HouseholdMemberEntity`**: M:N junction table connecting profiles to households with granular roles:
   - `HouseholdRole.OWNER`: Creator / primary manager.
   - `HouseholdRole.ADMIN`: Can modify household accounts and budgets.
   - `HouseholdRole.MEMBER`: Can record transactions.
   - `HouseholdRole.VIEWER`: Read-only reporting access.
4. **Account Ownership**:
   - **Personal Account:** `ownershipType = PERSONAL`, `householdId = NULL`. Visible strictly to the owner profile.
   - **Household Account:** `ownershipType = HOUSEHOLD`, `householdId = <household_id>`. Shared with members of that household.
