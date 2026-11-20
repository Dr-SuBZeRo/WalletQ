# WalletQ Financial Architecture & Accounting Rules

**Application:** WalletQ  
**Lead Architect:** SuBZeRo  
**Standard:** Double-Entry Foundation & Minor Unit Decimal Control  

---

## 1. Core Principle: Absolute Decimal Precision

Financial calculation errors commonly originate from binary floating-point representation (`Float` and `Double`), where values such as `0.1 + 0.2` yield `0.30000000000000004`.

**Non-Negotiable Rule:**  
In WalletQ, **`Float` and `Double` are strictly forbidden for monetary storage and balance arithmetic.**

---

## 2. Integer Minor Units (`Long`)

Every monetary amount is stored as a 64-bit signed integer (`Long minorUnits`) representing the smallest fractional denomination defined by the currency's ISO-4217 standard.

### Examples:
| Currency | Code | Decimals | Minor Unit Name | Major Amount | Stored `minorUnits` |
|---|---|---|---|---|---|
| Indian Rupee | `INR` | 2 | Paise | ₹ 250.75 | `25075L` |
| US Dollar | `USD` | 2 | Cent | $ 19.99 | `1999L` |
| Euro | `EUR` | 2 | Cent | € 100.00 | `10000L` |
| British Pound | `GBP` | 2 | Penny | £ 45.50 | `4550L` |
| UAE Dirham | `AED` | 2 | Fils | 50.25 AED | `5025L` |
| Japanese Yen | `JPY` | 0 | Yen | ¥ 5,000 | `5000L` |

---

## 3. Currency Metadata Specification

WalletQ does not assume all world currencies have two decimal places. The system maintains an explicit ISO registry:

```kotlin
data class Currency(
    val code: String,          // ISO-4217 3-letter code
    val displayName: String,   // Full human-readable name
    val symbol: String,        // Display symbol (e.g. ₹, $, €, ¥)
    val decimalPlaces: Int     // Exponent for fractional units (0, 2, 3)
)
```

Default reporting currency is set to **INR (`₹`)**.

---

## 4. Cross-Currency Arithmetic Safety

Monetary amounts of different currencies **must never be added or subtracted directly**.

```kotlin
val inr = Money(10000L, Currency.INR) // ₹ 100.00
val usd = Money(1000L, Currency.USD)  // $ 10.00

// FORBIDDEN - Throws IllegalArgumentException:
val total = inr + usd 
```

To sum multi-currency accounts, explicit exchange conversion into the user's reporting currency must occur using `BigDecimal` multiplier rates.

---

## 5. Controlled Decimal Calculations & Banker's Rounding

When performing multiplication (such as tax, percentage division, or currency conversion), calculations transition to `java.math.BigDecimal` using **Banker's Rounding** (`RoundingMode.HALF_EVEN`):
- Minimizes statistical bias in cumulative rounding operations.
- The result is immediately scaled back to the currency's defined `decimalPlaces` and stored as integer `minorUnits`.

---

## 6. Accounting Invariant (Double-Entry Mechanics for Phase 2)

When transactions are implemented in Phase 2:
1. **Transfers:** A transfer between Account A and Account B consists of equal and opposite debits and credits:
   $$\Delta \text{Balance}_A + \Delta \text{Balance}_B = 0$$
2. **Account Balances:** Stored balances must always be verifiable by computing the sum of initial balance and all historical transaction deltas.
