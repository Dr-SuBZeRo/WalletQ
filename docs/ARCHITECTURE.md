# WalletQ Architecture Specification

**Application:** WalletQ  
**Developer:** SuBZeRo  
**Package:** `com.walletq`  
**License:** Apache License 2.0  
**Phase:** 1 — Project Initialization & Architecture Baseline  

---

## 1. Architectural Philosophy & Principles

WalletQ is engineered as a privacy-first, offline-first personal and household financial management platform. 

The software architecture is governed by four core tenets:
1. **Uncompromising Privacy:** Financial data is personal and sensitive. WalletQ requires zero accounts, zero phone numbers, and has zero analytics or tracking SDKs. It does not declare or use the `android.permission.INTERNET` permission.
2. **Single Source of Truth (SSOT):** The local SQLite/Room database (`walletq.db`) is the sole authoritative state of truth on the device.
3. **Clean Architecture & Unidirectional Data Flow (UDF):**
   - **Presentation Layer (Jetpack Compose):** Consumes immutable UI state streams and dispatches user intentions as events. Contains zero financial calculation or database access logic.
   - **Domain Layer (Kotlin):** Pure business entities (`Money`, `Currency`, `OwnershipType`, `Account`, `Profile`) completely isolated from Android SDK dependencies.
   - **Data Layer (Room + DataStore):** Concrete implementations of persistence, schema definitions, indices, and preference management.
4. **Deterministic Financial Math:** Monetary amounts are stored as exact 64-bit signed integers representing minor units, entirely avoiding binary floating-point round-off errors.

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
|      Pure Kotlin Models (Money, Currency, Ownership)        |
|                Contracts & Business Rules                   |
+------------------------------+------------------------------+
                               ^
                               |
+-------------------------------------------------------------+
|                         Data Layer                          |
|         Room DAOs & Entities (Profile, Account, Currency)   |
|         DataStore Preferences (Theme, Default Currency)      |
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

## 3. Module Responsibilities & Planned Incremental Modularization

To ensure smooth compilation on resource-constrained development hardware (8 GB RAM), WalletQ initializes with a streamlined package architecture inside `:app` that maintains strict modular boundaries. 

As features expand in subsequent phases, packages will be incrementally extracted into dedicated Gradle modules without refactoring core logic:

| Target Module | Current Package | Responsibilities | Dependencies |
|---|---|---|---|
| `:core:model` | `com.walletq.core.model` | Pure Kotlin financial models (`Money`, `Currency`, `OwnershipType`), business rules. Zero Android dependencies. | Kotlinx Serialization |
| `:core:database` | `com.walletq.core.database` | Room database schema, SQLite DAOs, type converters, integrity callbacks. | Room, Kotlinx Coroutines |
| `:core:datastore` | `com.walletq.core.datastore` | Preferences DataStore repository for theme, dynamic colors, active profile. | DataStore Preferences |
| `:core:designsystem` | `com.walletq.core.designsystem`| Material 3 design tokens, typography, shapes, colors, reusable UI widgets. | Jetpack Compose M3 |
| `:core:navigation` | `com.walletq.core.navigation` | Navigation Compose definitions, top-level destination routes, bottom navigation. | Navigation Compose |
| `:feature:home` | `com.walletq.feature.home` | Overview dashboard, net worth summary, recent activity feed. | Core modules |
| `:feature:activity` | `com.walletq.feature.activity` | Transaction log, chronological list, transfer details. | Core modules |
| `:feature:accounts` | `com.walletq.feature.accounts` | Multi-account management (Cash, Bank, Savings, Cards). | Core modules |
| `:feature:reports` | `com.walletq.feature.reports` | Spending analytics, cash flow breakdown, budget charts. | Core modules |
| `:feature:more` | `com.walletq.feature.more` | Settings, theme toggle, currency selection, privacy audit. | Core modules |

---

## 4. Database Strategy & Foreign Key Integrity

The local SQLite database (`walletq.db`) is managed via AndroidX Room.

### Key Constraints:
1. **Foreign Key Enforcement:** SQLite does not enable foreign key enforcement by default. WalletQ explicitly executes `PRAGMA foreign_keys = ON;` inside the `RoomDatabase.Callback.onOpen` hook.
2. **Cascading Deletion:** If a parent profile is deleted, all associated accounts and financial records cascade delete to prevent orphaned data.
3. **Indexing:** Every foreign key column (e.g. `AccountEntity.profileId`) is explicitly indexed in Room to ensure performant queries.

---

## 5. Dual Ownership Model: Personal vs. Household

WalletQ natively distinguishes between individual finances and collective household finances:
- **`OwnershipType.PERSONAL`**: Belongs strictly to an individual profile.
- **`OwnershipType.HOUSEHOLD`**: Shared across household members on the same device.

All records retain foreign key association to their originating `ProfileEntity`. In Phase 1, profiles reside on the local device. No multi-device sync is assumed, keeping user data private and self-contained.
