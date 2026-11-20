package com.walletq.core.model

/**
 * Defines the ownership scope of a financial record or profile.
 * Every financial entity in WalletQ belongs either to an individual or a shared household.
 */
enum class OwnershipType {
    PERSONAL,
    HOUSEHOLD
}
