package com.walletq.feature.accounts.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.walletq.core.designsystem.WalletQVibrantPalette
import com.walletq.core.model.AccountType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AccountColorOption(
    val id: String,
    val label: String,
    val color: Color
)

data class AccountIconOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

object AccountVisuals {

    val colorOptions: List<AccountColorOption> = WalletQVibrantPalette.accountCardThemes.map {
        AccountColorOption(it.id, it.label, it.endColor)
    }

    val iconOptions: List<AccountIconOption> = listOf(
        AccountIconOption("BANK", "Bank", Icons.Outlined.AccountBalance),
        AccountIconOption("CASH", "Cash", Icons.Outlined.Payments),
        AccountIconOption("SAVINGS", "Savings", Icons.Outlined.Savings),
        AccountIconOption("CREDIT_CARD", "Card", Icons.Outlined.CreditCard),
        AccountIconOption("WALLET", "Wallet", Icons.Outlined.AccountBalanceWallet),
        AccountIconOption("OTHER", "Other", Icons.Outlined.Wallet)
    )

    val selectableAccountTypes: List<AccountType> = listOf(
        AccountType.CASH,
        AccountType.BANK,
        AccountType.SAVINGS,
        AccountType.CREDIT_CARD,
        AccountType.OTHER
    )

    fun resolveIcon(type: AccountType, iconIdentifier: String?): ImageVector {
        return when (iconIdentifier) {
            "BANK" -> Icons.Outlined.AccountBalance
            "CASH" -> Icons.Outlined.Payments
            "SAVINGS" -> Icons.Outlined.Savings
            "CREDIT_CARD" -> Icons.Outlined.CreditCard
            "WALLET" -> Icons.Outlined.AccountBalanceWallet
            "INVESTMENT" -> Icons.AutoMirrored.Outlined.TrendingUp
            "OTHER" -> Icons.Outlined.Wallet
            else -> when (type) {
                AccountType.CASH -> Icons.Outlined.Payments
                AccountType.BANK -> Icons.Outlined.AccountBalance
                AccountType.SAVINGS -> Icons.Outlined.Savings
                AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
                AccountType.INVESTMENT -> Icons.AutoMirrored.Outlined.TrendingUp
                AccountType.OTHER -> Icons.Outlined.AccountBalanceWallet
            }
        }
    }

    fun resolveColor(colorIdentifier: String?, fallback: Color): Color {
        return colorOptions.find { it.id == colorIdentifier }?.color ?: fallback
    }

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    fun formatDate(epochMs: Long): String {
        return Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }
}
