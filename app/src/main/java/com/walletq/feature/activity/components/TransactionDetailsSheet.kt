package com.walletq.feature.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.ExpenseRed
import com.walletq.core.designsystem.IncomeGreen
import com.walletq.core.designsystem.WarningAmber
import com.walletq.core.model.Account
import com.walletq.core.model.Category
import com.walletq.core.model.Household
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.LedgerEntryType
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsSheet(
    transaction: Transaction,
    accountsById: Map<String, Account>,
    categoriesById: Map<String, Category>,
    householdsById: Map<String, Household>,
    ledgerEntries: List<LedgerEntry>,
    isReversing: Boolean,
    onDismiss: () -> Unit,
    onConfirmReversal: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConfirmDialog by remember { mutableStateOf(false) }
    var reversalReason by remember { mutableStateOf("") }

    val sourceAccount = accountsById[transaction.accountId]
    val destAccount = transaction.transferDetails?.destinationAccountId?.let { accountsById[it] }
    val category = transaction.categoryId?.let { categoriesById[it] }
    val household = transaction.householdId?.let { householdsById[it] }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
    }
    val formattedDate = remember(transaction.transactionDateEpochMs) {
        Instant.ofEpochMilli(transaction.transactionDateEpochMs)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    val amountColor = when {
        transaction.status == TransactionStatus.REVERSED -> MaterialTheme.colorScheme.onSurfaceVariant
        transaction.type == TransactionType.INCOME -> IncomeGreen
        transaction.type == TransactionType.EXPENSE -> ExpenseRed
        else -> MaterialTheme.colorScheme.primary
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.spaceMedium)
                .padding(bottom = Dimensions.spaceLarge)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = transaction.type.name,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatusBadge(
                    text = transaction.status.name,
                    containerColor = if (transaction.status == TransactionStatus.POSTED) {
                        IncomeGreen.copy(alpha = 0.16f)
                    } else {
                        WarningAmber.copy(alpha = 0.2f)
                    },
                    contentColor = if (transaction.status == TransactionStatus.POSTED) {
                        IncomeGreen
                    } else {
                        WarningAmber
                    }
                )
            }

            Column {
                Text(
                    text = transaction.amount.format(),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider()

            // Core Details
            DetailRow(
                label = if (transaction.type == TransactionType.TRANSFER) "From Account" else "Account",
                value = sourceAccount?.let { "${it.name} (${it.currency.code})" } ?: transaction.accountId
            )

            if (transaction.type == TransactionType.TRANSFER && transaction.transferDetails != null) {
                val details = transaction.transferDetails
                DetailRow(
                    label = "To Account",
                    value = destAccount?.let { "${it.name} (${it.currency.code})" } ?: details.destinationAccountId
                )
                DetailRow(
                    label = "Destination Amount",
                    value = details.destinationAmount.format()
                )
                if (details.appliedExchangeRate != null &&
                    details.destinationAmount.currency.code != transaction.amount.currency.code
                ) {
                    DetailRow(
                        label = "Applied Exchange Rate",
                        value = "1 ${transaction.amount.currency.code} = ${details.appliedExchangeRate.toPlainString()} ${details.destinationAmount.currency.code}"
                    )
                }
            }

            if (category != null) {
                DetailRow(label = "Category", value = category.name)
            }

            DetailRow(label = "Date", value = formattedDate)

            DetailRow(
                label = "Ownership Scope",
                value = if (transaction.ownershipType == OwnershipType.HOUSEHOLD) {
                    "Household (${household?.name ?: transaction.householdId ?: "Shared"})"
                } else {
                    "Personal (${transaction.ownerProfileId})"
                }
            )

            if (!transaction.notes.isNullOrBlank()) {
                DetailRow(label = "Notes", value = transaction.notes)
            }

            // Audit & Reversal References
            if (transaction.originalTransactionId != null || transaction.reversalTransactionId != null) {
                HorizontalDivider()
                Text(
                    text = "Audit Trail",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (transaction.originalTransactionId != null) {
                    DetailRow(label = "Original Transaction ID", value = transaction.originalTransactionId)
                }
                if (transaction.reversalTransactionId != null) {
                    DetailRow(label = "Reversal Transaction ID", value = transaction.reversalTransactionId)
                }
            }

            // Double-Entry Ledger Entries
            if (ledgerEntries.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Double-Entry Ledger",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                ledgerEntries.forEach { entry ->
                    val entryAccount = accountsById[entry.accountId]?.name ?: entry.accountId
                    val signedFormatted = Money(entry.signedMinorUnits, entry.currency).format()
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entryAccount,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = entry.entryType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = signedFormatted,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (entry.entryType == LedgerEntryType.CREDIT) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // Reversal Action (Only for POSTED transactions; never physically delete)
            if (transaction.status == TransactionStatus.POSTED) {
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                OutlinedButton(
                    onClick = { showConfirmDialog = true },
                    enabled = !isReversing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.minTouchTarget)
                ) {
                    if (isReversing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Undo,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Reverse Transaction")
                    }
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This transaction has been reversed. Financial history is preserved in the immutable ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Transaction Reversal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                    Text(
                        text = "Reversing will create a compensating ledger entry to restore account balances while keeping the original record intact for auditability.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = reversalReason,
                        onValueChange = { reversalReason = it },
                        label = { Text("Reason (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmReversal(reversalReason)
                    }
                ) {
                    Text("Confirm Reversal", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}
