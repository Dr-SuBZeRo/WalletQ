package com.walletq.feature.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.component.WalletQBottomSheet
import com.walletq.core.designsystem.component.WalletQFilterChip
import com.walletq.core.designsystem.component.WalletQPrimaryButton
import com.walletq.core.designsystem.component.WalletQSecondaryButton
import com.walletq.core.model.Account
import com.walletq.core.model.Category
import com.walletq.core.model.TransactionType
import com.walletq.feature.activity.DateRangeFilter
import com.walletq.feature.activity.OwnershipFilter
import com.walletq.feature.activity.TransactionFilterState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityFilterSheet(
    filterState: TransactionFilterState,
    accounts: List<Account>,
    categories: List<Category>,
    onOwnershipFilterChanged: (OwnershipFilter) -> Unit,
    onTypeFilterChanged: (TransactionType?) -> Unit,
    onDateRangeFilterChanged: (DateRangeFilter) -> Unit,
    onAccountFilterChanged: (String?) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = WalletQThemeTokens.colors

    WalletQBottomSheet(
        title = "Filter Activity",
        onDismiss = onDismiss
    ) {
        // Ownership
        Text(
            text = "Ownership",
            style = MaterialTheme.typography.labelLarge,
            color = tokens.secondaryText
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
        ) {
            OwnershipFilter.entries.forEach { ownership ->
                WalletQFilterChip(
                    selected = filterState.ownershipFilter == ownership,
                    label = ownership.label,
                    onClick = { onOwnershipFilterChanged(ownership) }
                )
            }
        }

        // Transaction Type
        Text(
            text = "Transaction Type",
            style = MaterialTheme.typography.labelLarge,
            color = tokens.secondaryText
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
        ) {
            WalletQFilterChip(
                selected = filterState.selectedType == null,
                label = "All Types",
                onClick = { onTypeFilterChanged(null) }
            )
            TransactionType.entries.forEach { type ->
                val label = when (type) {
                    TransactionType.INCOME -> "Income"
                    TransactionType.EXPENSE -> "Expense"
                    TransactionType.TRANSFER -> "Transfer"
                }
                WalletQFilterChip(
                    selected = filterState.selectedType == type,
                    label = label,
                    onClick = {
                        onTypeFilterChanged(if (filterState.selectedType == type) null else type)
                    }
                )
            }
        }

        // Date Range
        Text(
            text = "Date Range",
            style = MaterialTheme.typography.labelLarge,
            color = tokens.secondaryText
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
        ) {
            DateRangeFilter.entries.forEach { range ->
                WalletQFilterChip(
                    selected = filterState.dateRangeFilter == range,
                    label = range.label,
                    onClick = { onDateRangeFilterChanged(range) }
                )
            }
        }

        // Account Filter
        if (accounts.isNotEmpty()) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.labelLarge,
                color = tokens.secondaryText
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
            ) {
                WalletQFilterChip(
                    selected = filterState.selectedAccountId == null,
                    label = "All Accounts",
                    onClick = { onAccountFilterChanged(null) }
                )
                accounts.forEach { account ->
                    WalletQFilterChip(
                        selected = filterState.selectedAccountId == account.id,
                        label = "${account.name} (${account.currency.code})",
                        onClick = {
                            onAccountFilterChanged(
                                if (filterState.selectedAccountId == account.id) null else account.id
                            )
                        }
                    )
                }
            }
        }

        // Category Filter
        if (categories.isNotEmpty()) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = tokens.secondaryText
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
            ) {
                WalletQFilterChip(
                    selected = filterState.selectedCategoryId == null,
                    label = "All Categories",
                    onClick = { onCategoryFilterChanged(null) }
                )
                categories.forEach { category ->
                    WalletQFilterChip(
                        selected = filterState.selectedCategoryId == category.id,
                        label = category.name,
                        onClick = {
                            onCategoryFilterChanged(
                                if (filterState.selectedCategoryId == category.id) null else category.id
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            WalletQSecondaryButton(
                text = "Clear All",
                onClick = onClearAll,
                modifier = Modifier.weight(1f)
            )
            WalletQPrimaryButton(
                text = "Apply",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
