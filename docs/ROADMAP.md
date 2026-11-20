# WalletQ Development Roadmap

**Project:** WalletQ  
**Developer:** SuBZeRo  
**Methodology:** Incremental Clean Architecture  

---

## Phase 1: Project Initialization & Architecture Baseline (Current)
- [x] Environment inspection (JDK 17, Android SDK 36, Gradle 8.13 wrapper).
- [x] Memory-optimized Gradle build toolchain (`-Xmx1536m`, SerialGC, in-process compiler).
- [x] Centralized dependencies in `gradle/libs.versions.toml`.
- [x] Material 3 Design System (Fintech Blue, Teal, Neutral palette, Light & Dark themes, Dynamic colors).
- [x] Room Database foundation (`WalletQDatabase`, `ProfileEntity`, `AccountEntity`, `CurrencyEntity`, DAOs).
- [x] SQLite foreign keys enforcement callback (`PRAGMA foreign_keys = ON;`).
- [x] DataStore preferences repository (Theme mode, Dynamic color, Default reporting currency).
- [x] Financial domain models (`Money`, `Currency`, `OwnershipType`, `AccountType`) with integer minor units.
- [x] Navigation Compose shell with 5 top-level destinations (Home, Activity, Accounts, Reports, More).
- [x] Verification of zero `android.permission.INTERNET` in `AndroidManifest.xml`.
- [x] Unit test suite verifying money math, precision, navigation, and theme state.
- [x] Comprehensive architecture, privacy, and financial documentation.

---

## Phase 2: Transaction Management Engine
- [ ] Transaction domain models (Income, Expense, Transfer).
- [ ] Room schema expansion: `TransactionEntity`, `CategoryEntity`.
- [ ] Double-entry ledger integrity logic.
- [ ] Transaction creation and editing Compose dialogs.
- [ ] Chronological transaction feed on `ActivityScreen`.

---

## Phase 3: Multi-Account & Profile Management
- [ ] Account management CRUD (Cash, Bank Accounts, Savings, Credit Cards).
- [ ] Profile switcher UI (Personal vs. Household).
- [ ] Per-account balance recalculation and history audit.
- [ ] Interactive `AccountsScreen` with account grouping and visual card tokens.

---

## Phase 4: Financial Reports, Budgets & Analytics
- [ ] Category budget definitions and alert thresholds.
- [ ] Income vs Expense cash flow analytics.
- [ ] Native Compose canvas charts and trend lines.
- [ ] Monthly financial health summaries on `ReportsScreen`.

---

## Phase 5: Encrypted Local Backup & Release Preparation
- [ ] AES-GCM encrypted local file export and import.
- [ ] Secure JSON/SQLite export to user-chosen storage locations via Android Storage Access Framework (SAF).
- [ ] ProGuard / R8 optimization and APK shrinking validation.
- [ ] Full automated UI test suite.
- [ ] Release signing and open-source distribution.
