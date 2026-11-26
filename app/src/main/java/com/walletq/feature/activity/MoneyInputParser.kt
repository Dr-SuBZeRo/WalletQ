package com.walletq.feature.activity

import com.walletq.core.model.Currency
import com.walletq.core.model.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure Kotlin parser for user-entered monetary strings and exchange rates.
 *
 * Strictly avoids Float and Double; uses [BigDecimal] to validate currency-specific
 * decimal precision and converts safely to integer minor units via [Money.fromBigDecimal].
 */
object MoneyInputParser {

    sealed interface ParseResult<out T> {
        data class Success<T>(val value: T) : ParseResult<T>
        data class Error(val message: String) : ParseResult<Nothing>
    }

    fun parseAmount(input: String, currency: Currency): ParseResult<Money> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Error("Amount is required.")
        }
        if (trimmed.contains('-') || trimmed.contains('+')) {
            return ParseResult.Error("Enter a positive amount without sign.")
        }
        if (trimmed.any { it.equals('e', ignoreCase = true) }) {
            return ParseResult.Error("Invalid numeric format.")
        }

        val decimal = try {
            BigDecimal(trimmed)
        } catch (_: NumberFormatException) {
            return ParseResult.Error("Enter a valid number.")
        }

        if (decimal <= BigDecimal.ZERO) {
            return ParseResult.Error("Amount must be greater than zero.")
        }

        val stripped = decimal.stripTrailingZeros()
        val effectiveScale = if (stripped.scale() < 0) 0 else stripped.scale()
        if (effectiveScale > currency.decimalPlaces) {
            return if (currency.decimalPlaces == 0) {
                ParseResult.Error("${currency.code} does not support decimal places.")
            } else {
                ParseResult.Error("${currency.code} supports at most ${currency.decimalPlaces} decimal places.")
            }
        }

        return try {
            val money = Money.fromBigDecimal(decimal, currency)
            if (!money.isPositive()) {
                ParseResult.Error("Amount must be greater than zero.")
            } else {
                ParseResult.Success(money)
            }
        } catch (_: ArithmeticException) {
            ParseResult.Error("Amount exceeds maximum supported limit.")
        }
    }

    fun parseExchangeRate(input: String): ParseResult<BigDecimal> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Error("Exchange rate is required for cross-currency transfers.")
        }
        if (trimmed.contains('-') || trimmed.contains('+')) {
            return ParseResult.Error("Exchange rate must be positive.")
        }
        if (trimmed.any { it.equals('e', ignoreCase = true) }) {
            return ParseResult.Error("Invalid exchange rate format.")
        }

        val rate = try {
            BigDecimal(trimmed)
        } catch (_: NumberFormatException) {
            return ParseResult.Error("Enter a valid numeric exchange rate.")
        }

        if (rate <= BigDecimal.ZERO) {
            return ParseResult.Error("Exchange rate must be greater than zero.")
        }

        return ParseResult.Success(rate)
    }

    fun computeDestinationPreview(
        sourceAmountInput: String,
        sourceCurrency: Currency,
        destinationCurrency: Currency,
        exchangeRateInput: String
    ): Money? {
        val sourceResult = parseAmount(sourceAmountInput, sourceCurrency)
        if (sourceResult !is ParseResult.Success) return null

        return if (sourceCurrency.code == destinationCurrency.code) {
            sourceResult.value
        } else {
            val rateResult = parseExchangeRate(exchangeRateInput)
            if (rateResult !is ParseResult.Success) return null
            try {
                sourceResult.value.convertTo(destinationCurrency, rateResult.value)
            } catch (_: ArithmeticException) {
                null
            }
        }
    }

    fun parseBalanceInput(
        input: String,
        currency: Currency,
        allowNegative: Boolean = false
    ): ParseResult<Money> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Success(Money.zero(currency))
        }
        if (!allowNegative && trimmed.contains('-')) {
            return ParseResult.Error("Opening balance cannot be negative for this account type.")
        }
        if (trimmed.contains('+')) {
            return ParseResult.Error("Enter a valid numeric balance without '+' sign.")
        }
        if (trimmed.any { it.equals('e', ignoreCase = true) }) {
            return ParseResult.Error("Invalid numeric format.")
        }

        val decimal = try {
            BigDecimal(trimmed)
        } catch (_: NumberFormatException) {
            return ParseResult.Error("Enter a valid number.")
        }

        if (!allowNegative && decimal < BigDecimal.ZERO) {
            return ParseResult.Error("Opening balance cannot be negative for this account type.")
        }

        val stripped = decimal.stripTrailingZeros()
        val effectiveScale = if (stripped.scale() < 0) 0 else stripped.scale()
        if (effectiveScale > currency.decimalPlaces) {
            return if (currency.decimalPlaces == 0) {
                ParseResult.Error("${currency.code} does not support decimal places.")
            } else {
                ParseResult.Error("${currency.code} supports at most ${currency.decimalPlaces} decimal places.")
            }
        }

        return try {
            ParseResult.Success(Money.fromBigDecimal(decimal, currency))
        } catch (_: ArithmeticException) {
            ParseResult.Error("Balance exceeds maximum supported limit.")
        }
    }

    fun formatExchangeRate(rate: BigDecimal): String {
        return rate.setScale(6, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString()
    }
}
