# WalletQ Phase Implementation Checklist

## Phase 1: Foundation & Baseline Architecture
- [x] Environment inspection and Gradle 8.13 configuration.
- [x] Material 3 Design System (Fintech Blue, Teal, Neutral).
- [x] Room Database schema V2 with foreign keys and migrations.
- [x] First-class Household relational model (`households`, `household_members`, roles).
- [x] Multi-currency metadata (INR, USD, EUR, GBP, AED, JPY).
- [x] DataStore preferences repository.
- [x] Zero network permission audit (`android.permission.INTERNET` absent).
- [x] Navigation Compose with 5 top-level destinations.

## Phase 2A: Financial Ledger Domain Engine
- [x] Define `TransactionType` (INCOME, EXPENSE, TRANSFER).
- [x] Define `TransactionStatus` (POSTED, REVERSED).
- [x] Define `LedgerEntryType` (CREDIT, DEBIT).
- [x] Define `TransferDetails` with optional `BigDecimalSerializer`.
- [x] Define `Transaction` domain model with ownership and audit fields.
- [x] Define `LedgerEntry` domain model with signed minor units.
- [x] Implement `LedgerEngine.validateTransaction` (positive amounts, currency match, ownership isolation).
- [x] Implement `LedgerEngine.generateLedgerEntries` (single entry for Income/Expense, paired entries for Transfer).
- [x] Implement same-currency transfer value conservation ($\Delta B_1 + \Delta B_2 = 0$).
- [x] Implement cross-currency transfer calculations with Banker's rounding (`HALF_EVEN`).
- [x] Implement `LedgerEngine.calculateAccountBalance` with checked arithmetic (`Math.addExact`).
- [x] Implement `LedgerEngine.calculateRunningBalances` chronological progression.
- [x] Implement `LedgerEngine.createReversal` preserving audit trail.
- [x] Implement `LedgerEngine.createReplacement` (reversal + replacement pairing).
- [x] Implement ownership validation: Personal vs Household isolation, cross-household blocking, authorized member validation.
- [x] Write comprehensive unit tests in `LedgerEngineTest.kt` (21 tests covering income, expense, transfers, overflow, reversals, replacements, edge cases for JPY & INR).
- [x] Update documentation (`docs/FINANCIAL_RULES.md`, `docs/ARCHITECTURE.md`, `docs/IMPLEMENTATION_CHECKLIST.md`).

## Phase 2B: Room Database Integration & Repository Layer
- [x] Create `Category` domain model with system category defaults.
- [x] Create `TransactionEntity` with minor units, transfer details, and audit fields.
- [x] Create `LedgerEntryEntity` with signed minor units and composite indices.
- [x] Create `CategoryEntity` with hierarchy and ownership references.
- [x] Implement TypeConverters for `TransactionType`, `TransactionStatus`, `LedgerEntryType`, and `BigDecimal`.
- [x] Implement `TransactionDao`, `LedgerDao`, and `CategoryDao`.
- [x] Update `AccountDao` with `updateCurrentBalance` and `HouseholdDao` with synchronous `getMembersForHousehold`.
- [x] Create bidirectional entity/domain mappers (`DatabaseMappers.kt`).
- [x] Implement `MIGRATION_2_3` in `WalletQDatabase` with non-destructive preservation and idempotent category seeding.
- [x] Export Room schema version 3 JSON (`app/schemas/com.walletq.core.database.WalletQDatabase/3.json`).
- [x] Implement `TransactionRunner` and `RoomTransactionRunner` for atomic database transactions.
- [x] Implement `TransactionRepository` and `TransactionRepositoryImpl` (validation via LedgerEngine, atomic writes, balance projections, reversals, replacements).
- [x] Implement `CategoryRepository` and `CategoryRepositoryImpl` (ownership guards, archival, delete restrictions).
- [x] Provide Hilt dependency injection bindings in `DatabaseModule` and `RepositoryModule`.
- [x] Write schema migration unit tests in `MigrationTest.kt` (table creation, indices, preservation, default category seeding).
- [x] Write repository unit tests in `TransactionRepositoryTest.kt` (income, expense, same-currency transfer, cross-currency transfer, reversal, replacement, rollback, ownership boundaries, historical balances).
- [x] Write category repository unit tests in `CategoryRepositoryTest.kt` (ownership boundaries, archival, deletion guards).
- [x] Comprehensive verification: `testDebugUnitTest`, `assembleDebug`, `lintDebug`.
- [x] Update documentation (`docs/DATABASE_SCHEMA.md`, `docs/ARCHITECTURE.md`, `docs/FINANCIAL_RULES.md`, `docs/IMPLEMENTATION_CHECKLIST.md`).

## Phase 2C: Transaction Management UI
- [x] Implement `MoneyInputParser` (`BigDecimal` currency precision validation, zero Float/Double).
- [x] Implement `TransactionViewModel` with `StateFlow<ActivityUiState>`, form states, filters, and duplicate submission prevention.
- [x] Implement `ActivityScreen` & `ActivityScreenContent` with chronological date grouping, search, and filters.
- [x] Implement `AddTransactionSheet` for Income and Expense entry with ownership and category validation.
- [x] Implement `TransferSheet` for same-currency and cross-currency transfers with conversion preview.
- [x] Implement `TransactionDetailsSheet` with double-entry ledger display and reversal confirmation dialog.
- [x] Add unit tests (`MoneyInputParserTest`, `TransactionViewModelTest`) and Compose UI tests (`ActivityScreenTest`).
- [x] Verify `testDebugUnitTest`, `assembleDebug`, `lintDebug` and update documentation.

## Phase 3A: Account Management Foundation
- [x] Extend `Account` and `AccountEntity` with `openingBalanceDateEpochMs`, `description`, `iconIdentifier`, and `colorIdentifier` (`MIGRATION_3_4`, Room DB v4).
- [x] Implement `AccountRepository` and `AccountRepositoryImpl` with ownership validation, duplicate-name checks, auditable balance adjustments, archival consistency guards, and deletion protection.
- [x] Enforce archived-account exclusion in new transactions (`TransactionRepositoryImpl` & `TransactionViewModel`) while preserving historical display (`accountsById`).
- [x] Implement `AccountViewModel`, `AccountsUiState`, `AccountsScreen`, `AddEditAccountSheet`, `AccountDetailsSheet`, and `AdjustBalanceSheet`.
- [x] Add `AccountRepositoryTest`, `AccountViewModelTest`, `MIGRATION_3_4` tests, and archived-account regression tests in `TransactionRepositoryTest` and `TransactionViewModelTest`.
- [x] Verify `testDebugUnitTest`, `assembleDebug`, `lintDebug` and update documentation.

## Phase 3B: Account Integrity & Reconciliation
- [x] Audit Credit Card signed-balance accounting (purchases, payments, refunds, liability, transfers, reversals) and document model & limitations in `docs/FINANCIAL_RULES.md`.
- [x] Implement read-only `AccountReconciliationEngine` and `AccountReconciliationReport` detecting projection mismatches, missing/duplicate entries, currency mismatches, count inconsistencies, and incomplete reversals.
- [x] Integrate `reconcileAccount` and `observeAccountReconciliation` into `AccountRepository`, `AccountViewModel`, and `AccountDetailsSheet`.
- [x] Add `AccountReconciliationEngineTest` and `CreditCardAccountingTest`.
- [x] Verify `testDebugUnitTest`, `assembleDebug`, `lintDebug` and update documentation.

## Phase 4A: Budget Management Foundation
- [x] Create `Budget`, `BudgetPeriod`, `BudgetCategoryAllocation`, `BudgetSpendingSummary`, and `BudgetCalculationEngine`.
- [x] Add `BudgetEntity`, `BudgetCategoryAllocationEntity`, `BudgetDao`, `MIGRATION_4_5` (Room DB v5), and `5.json` schema export.
- [x] Implement `BudgetRepository` & `BudgetRepositoryImpl` with expense-only category validation, duplicate prevention, and household membership authorization.
- [x] Implement `BudgetViewModel`, `BudgetsUiState`, `BudgetsScreen`, and `AddEditBudgetSheet` (secondary route from `MoreScreen`).
- [x] Add `BudgetCalculationEngineTest`, `BudgetRepositoryTest`, `BudgetViewModelTest`, and `MIGRATION_4_5` test in `MigrationTest`.
- [x] Verify `testDebugUnitTest`, `assembleDebug`, `lintDebug` and update documentation.

## UI/UX Redesign Phase A: Midnight Luxe Design System
- [x] Implement Midnight Luxe Dark & coordinated Light colour system + `WalletQThemeTokens` (`LocalWalletQColors`).
- [x] Refine `Typography` (with `WalletQMonetaryTypography` tabular figures), `Dimensions` (4dp grid), and `Shapes`.
- [x] Implement `WalletQMotion` with reduced-motion support (`rememberReducedMotion`).
- [x] Build reusable Midnight Luxe components (`WalletQCard`, `WalletQSummaryCard`, `WalletQSectionHeader`, `WalletQButtons`, `WalletQInputs`, `WalletQStatusIndicator`, `WalletQLoadingPlaceholder`, `WalletQOverlays`, `WalletQFinancialRows`).
- [x] Redesign 5-destination bottom navigation (`WalletQBottomBar`) with system inset handling and `WalletQNavHost` screen transitions.
- [x] Verify `testDebugUnitTest`, `assembleDebug`, `lintDebug` and update documentation.

## UI/UX Redesign Phase B1: IvyWallet-Inspired Home & Accounts Transformation
- [x] Extend `Color.kt` with vibrant accent palette (`VibrantEmerald`, `VibrantTurquoise`, `VibrantPurple`, `VibrantElectricBlue`, `VibrantWarmYellow`, `VibrantCoral`, `VibrantPink`) and `WalletQVibrantPalette.accountCardThemes`.
- [x] Implement `HomeViewModel` & `HomeUiState` backed by real `AccountRepository`, `TransactionRepository`, `BudgetRepository`, and `CategoryRepository` flows (zero fabricated data).
- [x] Redesign `HomeScreen` (removed developer architecture cards; added greeting, month & ownership selectors, Net Worth hero card with visibility crossfade, vibrant Income & Expense cards, Cash Flow, Quick Actions, Account Overview, Budget Overview, and Recent Activity).
- [x] Redesign `AccountsScreen` with vibrant gradient account cards, currency-grouped summaries, credit card liability presentation, and actual monthly income/expense summaries per account.
- [x] Upgrade `WalletQBottomBar` with integrated central Add action opening Quick Action sheet wired to real transaction/account workflows.
- [x] Add `HomeViewModelTest` and verify `testDebugUnitTest`, `assembleDebug`, `lintDebug`.

## UI/UX Redesign Phase B2: Activity Screen Redesign
- [x] Extend `TransactionFilterState` (`selectedCategoryId`, `activeFilterCount`) and `ActivityUiState` (`ActivityMonthlySummary`) in `TransactionViewModel`.
- [x] Redesign `ActivityScreen` with expandable search, compact monthly summary card, horizontally scrollable quick filter chips, dedicated `ActivityFilterSheet`, and removal of competing FABs.
- [x] Redesign `TransactionListItem` with vibrant semantic colours, category icon badges, time labels, tabular numerals, and subdued reversed status.
- [x] Add `TransactionViewModelTest` coverage and verify `testDebugUnitTest`, `assembleDebug`, `lintDebug`.
