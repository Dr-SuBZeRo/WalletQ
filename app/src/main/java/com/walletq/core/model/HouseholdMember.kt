package com.walletq.core.model

/**
 * Junction domain model representing the membership of a Profile in a Household.
 * Supports multiple household memberships per personal profile.
 */
data class HouseholdMember(
    val householdId: String,
    val profileId: String,
    val role: HouseholdRole,
    val joinedAtEpochMs: Long
)
