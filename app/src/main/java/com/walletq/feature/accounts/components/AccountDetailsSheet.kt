package com.walletq.feature.accounts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.ExpenseRed
import com.walletq.core.designsystem.IncomeGreen
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.model.Account
import com.walletq.core.model.Household
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsSheet(
    account: Account,
    household: Household?,
    ledgerDerivedBalanceMinorUnits: Long?,
    transactions: List<Transaction>,
    onEditClick: () -> Unit,
    onAdjustBalanceClick: () -> Unit,
    onArchiveConfirm: () -> Unit,
    onUnarchiveClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showArchiveConfirm by remember { mutableStateOf(false) }

    val accentColor = AccountVisuals.resolveColor(
        account.colorIdentifier,
        MaterialTheme.colorScheme.primary
    )
    val displayBalanceMinor = ledgerDerivedBalanceMinorUnits ?: account.currentBalanceMinorUnits
    val currentMoney = Money(displayBalanceMinor, account.currency)
    val openingMoney = Money(account.initialBalanceMinorUnits, account.currency)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.spaceMedium)
                .padding(bottom = Dimensions.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountVisuals.resolveIcon(account.type, account.iconIdentifier),
                        contentDescription = null,
                        tint = accentColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    val ownershipLabel = if (account.ownershipType == OwnershipType.HOUSEHOLD) {
                        "Household${household?.let { " • ${it.name}" } ?: ""}"
                    } else {
                        "Personal"
                    }
                    Text(
                        text = "${account.type.displayName} • $ownershipLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (account.isArchived) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Archived",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Balance & Opening Balance Card
            WalletQCard {
                Text(
                    text = "Current Ledger Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentMoney.format(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (currentMoney.isNegative()) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Opening Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = openingMoney.format(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Opening Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = AccountVisuals.formatDate(account.openingBalanceDateEpochMs),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (!account.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                    Text(
                        text = account.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Edit")
                }
                if (!account.isArchived) {
                    OutlinedButton(
                        onClick = onAdjustBalanceClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Adjust")
                    }
                }
            }

            if (account.isArchived) {
                Button(
                    onClick = onUnarchiveClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Unarchive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(Dimensions.spaceSmall))
                    Text("Restore Account")
                }
            } else {
                OutlinedButton(
                    onClick = { showArchiveConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(Dimensions.spaceSmall))
                    Text("Archive Account")
                }
            }

            // Account-specific Transaction History
            Text(
                text = "Transaction History (${transactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions recorded for this account yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                    transactions.take(30).forEach { tx ->
                        AccountTransactionRow(
                            transaction = tx,
                            accountId = account.id
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        }
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Archive '${account.name}'?") },
            text = {
                Text(
                    "Archived accounts cannot be selected for new transactions, " +
                        "but all historical transactions and ledger entries remain preserved."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveConfirm = false
                        onArchiveConfirm()
                    }
                ) {
                    Text("Archive", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AccountTransactionRow(
    transaction: Transaction,
    accountId: String
) {
    val isInflow = when (transaction.type) {
        TransactionType.INCOME -> true
        TransactionType.EXPENSE -> false
        TransactionType.TRANSFER -> transaction.transferDetails?.destinationAccountId == accountId
    }
    val displayAmount = if (transaction.type == TransactionType.TRANSFER &&
        transaction.transferDetails?.destinationAccountId == accountId
    ) {
        transaction.transferDetails.destinationAmount
    } else {
        transaction.amount
    }

    val isReversed = transaction.status == TransactionStatus.REVERSED
    val amountColor = when {
        isReversed -> MaterialTheme.colorScheme.onSurfaceVariant
        isInflow -> IncomeGreen
        else -> ExpenseRed
    }
    val prefix = if (isInflow) "+" else "-"

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.spaceMedium, vertical = Dimensions.spaceSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isReversed) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = AccountVisuals.formatDate(transaction.transactionDateEpochMs) +
                        if (isReversed) " • Reversed" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$prefix${displayAmount.format()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                textDecoration = if (isReversed) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}
