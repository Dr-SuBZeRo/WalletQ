package com.walletq.core.model

/**
 * Domain model representing a financial account (e.g. Cash, Checking Bank, Savings).
 */
data class Account(
    val id: String,
    val profileId: String,
    val name: String,
    val type: AccountType,
    val currency: Currency,
    val initialBalanceMinorUnits: Long,
    val currentBalanceMinorUnits: Long,
    val isArchived: Boolean,
    val createdAtEpochMs: Long
)
