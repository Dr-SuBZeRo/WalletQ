package com.walletq.core.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Transfer details required for moving funds between two accounts.
 *
 * For same-currency transfers:
 * - [destinationAmount] matches the source transaction amount.
 * - [appliedExchangeRate] is null or BigDecimal.ONE.
 *
 * For cross-currency transfers:
 * - [destinationAmount] is in the destination account's currency.
 * - [appliedExchangeRate] records the explicit exchange multiplier applied.
 * - [exchangeRateReferenceId] optionally links to historical rate storage.
 */
@Serializable
data class TransferDetails(
    val destinationAccountId: String,
    val destinationAmount: Money,
    @Serializable(with = BigDecimalSerializer::class)
    val appliedExchangeRate: BigDecimal? = null,
    val exchangeRateReferenceId: String? = null
)
