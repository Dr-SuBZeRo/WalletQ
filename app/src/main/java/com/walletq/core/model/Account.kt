package com.walletq.core.model

/**
 * Domain model representing a financial account (e.g. Cash, Checking Bank, Savings).
 *
 * Supports both Personal ownership ([ownershipType] == PERSONAL)
 * and Household ownership ([ownershipType] == HOUSEHOLD, linked to [householdId]).
 */
data class Account(
    val id: String,
    val profileId: String,
    val householdId: String? = null,
    val ownershipType: OwnershipType = OwnershipType.PERSONAL,
    val name: String,
    val type: AccountType,
    val currency: Currency,
    val initialBalanceMinorUnits: Long,
    val currentBalanceMinorUnits: Long,
    val isArchived: Boolean,
    val createdAtEpochMs: Long
)
