package com.walletq.core.model

import java.math.BigDecimal

/**
 * Domain model representing an exchange rate between two currencies at a point in time.
 */
data class ExchangeRate(
    val id: String,
    val baseCurrencyCode: String,
    val targetCurrencyCode: String,
    val rate: BigDecimal,
    val timestampEpochMs: Long
)
