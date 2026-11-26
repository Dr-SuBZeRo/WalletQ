package com.walletq.core.model

/**
 * Enumeration of supported account types in WalletQ.
 */
enum class AccountType(val displayName: String) {
    CASH("Cash"),
    BANK("Bank Account"),
    SAVINGS("Savings Account"),
    CREDIT_CARD("Credit Card"),
    INVESTMENT("Investment"),
    OTHER("Other")
}
