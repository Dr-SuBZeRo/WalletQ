package com.walletq.core.model

/**
 * Profile entity representing an individual user or a shared household profile.
 */
data class Profile(
    val id: String,
    val name: String,
    val type: OwnershipType,
    val isDefault: Boolean,
    val createdAtEpochMs: Long
)
