# WalletQ Financial Architecture & Accounting Rules

**Application:** WalletQ  
**Lead Architect:** SuBZeRo  
**Standard:** Double-Entry Foundation, Minor Units, & Historical Exchange Rates  

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

## 3. Currency Registry & Variable Precision

| Currency | Code | Decimals | Minor Unit Name | Major Amount | Stored `minorUnits` |
|---|---|---|---|---|---|
| Indian Rupee | `INR` | 2 | Paise | ₹ 250.75 | `25075L` |
| US Dollar | `USD` | 2 | Cent | $ 19.99 | `1999L` |
| Euro | `EUR` | 2 | Cent | € 100.00 | `10000L` |
| British Pound | `GBP` | 2 | Penny | £ 45.50 | `4550L` |
| UAE Dirham | `AED` | 2 | Fils | 50.25 AED | `5025L` |
| Japanese Yen | `JPY` | 0 | Yen | ¥ 5,000 | `5000L` |

Default reporting currency is set to **INR (`₹`)**.

---

## 4. Historical Exchange Rate Storage

WalletQ persists historical exchange rates in the local SQLite table `exchange_rates`:
- `baseCurrencyCode`: Foreign key to `currencies(code)`
- `targetCurrencyCode`: Foreign key to `currencies(code)`
- `rate`: Exact `BigDecimal` string representation (e.g. `"83.50000000"`) avoiding floating-point precision loss
- `timestampEpochMs`: Timestamp of the exchange rate quote

---

## 5. Controlled Decimal Calculations & Banker's Rounding

When performing percentage division, ratio scaling, or currency conversion:
- Calculations transition to `java.math.BigDecimal` using **Banker's Rounding** (`RoundingMode.HALF_EVEN`).
- Equidistant `.5` fractions round to the nearest even integer, eliminating statistical bias in financial accumulations.
- Results are immediately rescaled to the currency's integer minor units.
