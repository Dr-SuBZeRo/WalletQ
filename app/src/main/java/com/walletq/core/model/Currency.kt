package com.walletq.core.model

import kotlinx.serialization.Serializable

/**
 * Currency metadata model representing ISO-4217 currencies.
 *
 * Financial Rule: Not all currencies utilize 2 decimal places.
 * Currencies such as JPY have 0 decimal places, while others like BHD or KWD have 3.
 * WalletQ models decimal places explicitly to ensure zero-loss integer storage.
 */
@Serializable
data class Currency(
    val code: String,
    val displayName: String,
    val symbol: String,
    val decimalPlaces: Int
) {
    /**
     * Multiplication factor from major currency units to minor units.
     * E.g., for INR (2 decimals), factor is 100.
     * For JPY (0 decimals), factor is 1.
     */
    val minorUnitFactor: Long
        get() {
            var factor = 1L
            repeat(decimalPlaces) { factor *= 10L }
            return factor
        }

    companion object {
        val INR = Currency(
            code = "INR",
            displayName = "Indian Rupee",
            symbol = "₹",
            decimalPlaces = 2
        )

        val USD = Currency(
            code = "USD",
            displayName = "US Dollar",
            symbol = "$",
            decimalPlaces = 2
        )

        val EUR = Currency(
            code = "EUR",
            displayName = "Euro",
            symbol = "€",
            decimalPlaces = 2
        )

        val GBP = Currency(
            code = "GBP",
            displayName = "British Pound",
            symbol = "£",
            decimalPlaces = 2
        )

        val AED = Currency(
            code = "AED",
            displayName = "UAE Dirham",
            symbol = "AED",
            decimalPlaces = 2
        )

        val JPY = Currency(
            code = "JPY",
            displayName = "Japanese Yen",
            symbol = "¥",
            decimalPlaces = 0
        )

        /**
         * Default reporting currency for WalletQ as specified in product requirements.
         */
        val DEFAULT_REPORTING_CURRENCY = INR

        val SUPPORTED_CURRENCIES = listOf(INR, USD, EUR, GBP, AED, JPY)

        fun findByCode(code: String): Currency {
            return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
                ?: Currency(
                    code = code.uppercase(),
                    displayName = code.uppercase(),
                    symbol = code.uppercase(),
                    decimalPlaces = 2
                )
        }
    }
}
