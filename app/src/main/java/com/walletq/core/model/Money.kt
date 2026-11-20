package com.walletq.core.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Immutable financial value type representing money in a specific currency.
 *
 * Core Financial Rules:
 * 1. Financial amounts MUST NEVER be stored as Float or Double due to binary floating-point rounding errors.
 * 2. Money is strictly stored as an exact 64-bit signed integer ([minorUnits]), representing the smallest
 *    divisible unit of that currency (e.g., Paise for INR, Cents for USD, Yen for JPY).
 * 3. Cross-currency addition or subtraction without explicit conversion is strictly prohibited.
 * 4. Decimal calculations (e.g. conversions, percentages) use [BigDecimal] with [RoundingMode.HALF_EVEN] (Banker's rounding).
 */
@Serializable
data class Money(
    val minorUnits: Long,
    val currency: Currency
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        require(currency.code == other.currency.code) {
            "Cannot add money in different currencies: '${currency.code}' and '${other.currency.code}'. Explicit currency conversion is required."
        }
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    operator fun minus(other: Money): Money {
        require(currency.code == other.currency.code) {
            "Cannot subtract money in different currencies: '${currency.code}' and '${other.currency.code}'. Explicit currency conversion is required."
        }
        return copy(minorUnits = Math.subtractExact(minorUnits, other.minorUnits))
    }

    operator fun unaryMinus(): Money = copy(minorUnits = -minorUnits)

    operator fun times(multiplier: Long): Money {
        return copy(minorUnits = Math.multiplyExact(minorUnits, multiplier))
    }

    /**
     * Controlled decimal multiplication for exchange rates, interest, or ratio calculations.
     * Uses Banker's Rounding ([RoundingMode.HALF_EVEN]).
     */
    operator fun times(factor: BigDecimal): Money {
        val currentDecimal = toBigDecimal()
        val result = currentDecimal.multiply(factor).setScale(currency.decimalPlaces, RoundingMode.HALF_EVEN)
        return fromBigDecimal(result, currency)
    }

    fun isZero(): Boolean = minorUnits == 0L

    fun isPositive(): Boolean = minorUnits > 0L

    fun isNegative(): Boolean = minorUnits < 0L

    fun toBigDecimal(): BigDecimal {
        return if (currency.decimalPlaces == 0) {
            BigDecimal.valueOf(minorUnits)
        } else {
            BigDecimal.valueOf(minorUnits).movePointLeft(currency.decimalPlaces)
        }
    }

    fun format(includeSymbol: Boolean = true): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = if (currency.decimalPlaces > 0) {
            "#,##0." + "0".repeat(currency.decimalPlaces)
        } else {
            "#,##0"
        }
        val decimalFormat = DecimalFormat(pattern, symbols)
        val formattedNumber = decimalFormat.format(toBigDecimal())
        return if (includeSymbol) {
            "${currency.symbol} $formattedNumber"
        } else {
            formattedNumber
        }
    }

    override fun compareTo(other: Money): Int {
        require(currency.code == other.currency.code) {
            "Cannot compare money across different currencies: '${currency.code}' and '${other.currency.code}'"
        }
        return minorUnits.compareTo(other.minorUnits)
    }

    companion object {
        fun zero(currency: Currency = Currency.DEFAULT_REPORTING_CURRENCY): Money {
            return Money(0L, currency)
        }

        fun fromMajorUnits(majorUnits: Long, currency: Currency): Money {
            val minor = Math.multiplyExact(majorUnits, currency.minorUnitFactor)
            return Money(minor, currency)
        }

        fun fromBigDecimal(amount: BigDecimal, currency: Currency): Money {
            val scaled = amount.setScale(currency.decimalPlaces, RoundingMode.HALF_EVEN)
            val minorUnits = scaled.movePointRight(currency.decimalPlaces).longValueExact()
            return Money(minorUnits, currency)
        }
    }
}
