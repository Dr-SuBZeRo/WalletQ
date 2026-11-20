package com.walletq.core.model

/**
 * Domain entity representing a Household unit.
 * A Household is a financial and organizational boundary comprising one or more members.
 */
data class Household(
    val id: String,
    val name: String,
    val description: String?,
    val createdAtEpochMs: Long
)
