# WalletQ Financial Architecture & Accounting Rules

**Application:** WalletQ  
**Lead Architect:** SuBZeRo  
**Standard:** Double-Entry Foundation, Minor Units, Historical Exchange Rates, & Immutable Ledger  

---

## 1. Core Principle: Absolute Decimal Precision

Financial calculation errors commonly originate from binary floating-point representation (`Float` and `Double`), where values such as `0.1 + 0.2` yield `0.30000000000000004`.

**Non-Negotiable Rule:**  
In WalletQ, **`Float` and `Double` are strictly forbidden for monetary storage and balance arithmetic.**

---

## 2. Integer Minor Units (`Long`) & Arithmetic Safety

Every monetary amount is stored as a 64-bit signed integer (`Long minorUnits`) representing the smallest fractional denomination defined by the currency's ISO-4217 standard.

### Arithmetic Safeguards:
- **`plus(other)` / `minus(other)`**: Uses `Math.addExact` and `Math.subtractExact`. Cross-currency math without explicit conversion is strictly prohibited and throws `IllegalArgumentException`.
- **`unaryMinus()`**: Uses `Math.negateExact(minorUnits)` preventing silent overflow when `minorUnits == Long.MIN_VALUE`.
- **`div(divisor: Long)`**: Scalar integer division utilizing Banker's Rounding (`RoundingMode.HALF_EVEN`) on fractional minor units. Division by zero throws `IllegalArgumentException`.
- **`div(other: Money)`**: Computes precise dimensionless `BigDecimal` ratio (scaled to 8 decimal places with `HALF_EVEN`).
- **`convertTo(targetCurrency, exchangeRate)`**: Controlled cross-currency conversion scaling to target currency's precision using `RoundingMode.HALF_EVEN`.

---

## 3. Financial Ledger & Double-Entry Accounting Engine

WalletQ employs an immutable financial event ledger architecture (`LedgerEngine`):

### 3.1. Transaction Types & Ledger Mapping
1. **`INCOME`**:
   - Represents an external inflow into an account.
   - Generates **1 `CREDIT`** ledger entry on the target account.
   - Increases account balance: $\text{Balance}_{\text{new}} = \text{Balance}_{\text{old}} + \text{Amount}$.
2. **`EXPENSE`**:
   - Represents an outflow from an account.
   - Generates **1 `DEBIT`** ledger entry on the source account.
   - Decreases account balance: $\text{Balance}_{\text{new}} = \text{Balance}_{\text{old}} - \text{Amount}$.
3. **`TRANSFER`**:
   - Reallocates funds between two distinct accounts.
   - **Never counts as income or expense.**
   - Generates **2 paired ledger entries**:
     - **`DEBIT`** on the Source Account ($- \text{Amount}$).
     - **`CREDIT`** on the Destination Account ($+ \text{DestinationAmount}$).

### 3.2. Transfer Value Conservation & Currency Rules
- **Same-Currency Transfer:**
  $$\Delta \text{Balance}_{\text{source}} + \Delta \text{Balance}_{\text{destination}} = 0$$
  The total net worth of the user across both accounts remains strictly unchanged.
- **Cross-Currency Transfer:**
  - Stored with explicit `TransferDetails`: `sourceAmount`, `destinationAmount`, `appliedExchangeRate`, and optional `exchangeRateReferenceId`.
  - Destination amount is deterministically derived:
    $$\text{DestinationAmount} = \text{SourceAmount} \times \text{ExchangeRate}$$
    rounded to the destination currency's decimal places via Banker's Rounding (`HALF_EVEN`).
  - Source and destination amounts are never summed across differing currencies.

---

## 4. Account Balance Derivation & Running Balances

Account balances are derived deterministically from the opening balance and the sum of ledger entries:
$$\text{CurrentBalance} = \text{InitialBalance} + \sum_{i=1}^{n} \text{SignedMinorUnits}_i$$
- Handled with checked operations (`Math.addExact`) to prevent silent integer wrap-around.
- `calculateRunningBalances` yields the chronological sequence of account balances over time.

---

## 5. Financial Corrections & Audit Trail (Reversals & Replacements)

In double-entry ledger bookkeeping, posted financial records are **never silently overwritten or deleted**.

1. **Transaction Reversal:**
   - The original transaction status transitions to `REVERSED` with a link to `reversalTransactionId`.
   - A compensating transaction (`POSTED`, referencing `originalTransactionId`) is recorded with inverted ledger entries:
     - Income reversal creates a `DEBIT` entry ($- \text{Amount}$).
     - Expense reversal creates a `CREDIT` entry ($+ \text{Amount}$).
     - Transfer reversal credits the source ($+ \text{Amount}$) and debits the destination ($- \text{DestinationAmount}$).
   - The combined historical ledger entries naturally zero out the original transaction.
2. **Replacement / Correction:**
   - To correct an error (e.g. wrong amount, wrong category), the engine executes an atomic correction:
     1. Reverses the original transaction with compensating entries.
     2. Posts the replacement transaction with `originalTransactionId` pointing to the original transaction.
   - Both the error and the correction remain permanently visible in the audit trail.

---

## 6. Ownership & Boundary Isolation Rules

Every transaction and transfer is validated against strict ownership boundaries:
1. **Personal Accounts:** Can only be modified by transactions owned by the same personal profile (`transaction.ownerProfileId == account.profileId`, `householdId == null`).
2. **Household Accounts:** Can only be modified by transactions belonging to the same household (`transaction.householdId == account.householdId`).
3. **Personal Transfers:** Direct transfers between two personal accounts are restricted to accounts belonging to the **same personal profile**.
4. **Household Transfers:** Direct transfers between two household accounts are restricted to the **same household**.
5. **Mixed Transfers (Personal $\leftrightarrow$ Household):** Permitted only when the personal account owner is an active member of the target household.

---

## 7. Repository Transaction Boundaries & Persistence Rules (Phase 2B)

1. **Atomic Database Writes:**
   - Transaction insertion, ledger entry generation, and account balance updates are executed within a single SQLite transaction (`database.withTransaction`).
   - If any step fails (validation error, constraint violation, overflow), the entire transaction rolls back.
   - A transaction cannot exist without its required ledger entries.
2. **Deletion Policies & Foreign Key Protections:**
   - `accounts`: Foreign key deletion policy is `ON DELETE RESTRICT` on transactions and ledger entries. Accounts with transaction history cannot be deleted; they must be archived.
   - `categories`: Foreign key deletion policy is `ON DELETE RESTRICT` on transactions. Categories referenced by transactions cannot be physically deleted; they must be archived.
   - `currencies`: Foreign key deletion policy is `ON DELETE RESTRICT` on accounts, transactions, and ledger entries.
3. **Idempotent Category Seeding:**
   - Default system categories are seeded using `INSERT OR IGNORE` in both initial database creation and version 2-to-3 migration.

---

## 8. Credit Card Accounting & Account Reconciliation (Phase 3B)

1. **Credit Card Signed-Balance Model:**
   - WalletQ uses a unified signed net-worth balance convention across all accounts:
     - **Purchases (`EXPENSE`):** Generate a `DEBIT` ($-\text{Amount}$), moving a credit card balance negative (increasing outstanding liability).
     - **Payments (`TRANSFER` from Bank/Cash to Credit Card):** Generate a `DEBIT` on the bank account and a `CREDIT` ($+\text{Amount}$) on the credit card, reducing outstanding liability without double-counting expenses.
     - **Refunds (`INCOME` or `REVERSAL`):** Generate a `CREDIT` ($+\text{Amount}$), reducing outstanding liability (or creating a positive credit surplus if over-refunded).
     - **Outstanding Liability:** $\max(0, -\text{CurrentBalance})$.
   - **Documented Limitations:** Statements/billing cycles, APR, and credit limits are not yet stored on `AccountEntity`; standalone `INCOME` refunds count as inflows on the card unless recorded via `reverseTransaction`.

2. **Read-Only Balance Reconciliation (`AccountReconciliationEngine`):**
   - Computes $\text{ExpectedBalance} = \text{InitialBalance} + \sum \text{LedgerDelta}$ and compares it against the stored `currentBalanceMinorUnits`.
   - Detects projection mismatches, missing ledger entries, duplicate ledger entries, currency mismatches, inconsistent transaction-to-ledger counts, and incomplete reversals.
   - Never silently mutates or overwrites balances; corrections must use the auditable `adjustBalance` transaction workflow.
