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
