package com.walletq.feature.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.model.Account
import com.walletq.feature.activity.TransferFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheet(
    formState: TransferFormState,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSourceAccountSelected: (String) -> Unit,
    onDestinationAccountSelected: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onExchangeRateChanged: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sourceAccount = accounts.find { it.id == formState.sourceAccountId }
    val destAccount = accounts.find { it.id == formState.destinationAccountId }

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
            Text(
                text = "Transfer Funds",
                style = MaterialTheme.typography.headlineSmall
            )

            if (accounts.size < 2) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "At least two active accounts are required to perform a transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Dimensions.spaceMedium)
                    )
                }
            }

            // Source Account
            AccountSelectorDropdown(
                label = "From Account (Source)",
                accounts = accounts,
                selectedAccountId = formState.sourceAccountId,
                onAccountSelected = onSourceAccountSelected
            )

            // Destination Account
            AccountSelectorDropdown(
                label = "To Account (Destination)",
                accounts = accounts,
                selectedAccountId = formState.destinationAccountId,
                onAccountSelected = onDestinationAccountSelected
            )

            // Source Amount
            CurrencyAmountTextField(
                value = formState.sourceAmountInput,
                onValueChange = onAmountChanged,
                currency = sourceAccount?.currency,
                label = "Source Amount",
                isError = formState.validationError?.contains("Amount", ignoreCase = true) == true
            )

            // Cross-currency vs Same-currency Section
            if (formState.isCrossCurrency && sourceAccount != null && destAccount != null) {
                OutlinedTextField(
                    value = formState.exchangeRateInput,
                    onValueChange = { next ->
                        if (next.all { it.isDigit() || it == '.' } && next.count { it == '.' } <= 1) {
                            onExchangeRateChanged(next)
                        }
                    },
                    label = { Text("Applied Exchange Rate") },
                    prefix = { Text("1 ${sourceAccount.currency.code} = ") },
                    suffix = { Text(destAccount.currency.code) },
                    supportingText = {
                        Text(
                            if (formState.exchangeRateReferenceId != null) {
                                "Prefilled from local historical rate (editable)"
                            } else {
                                "Enter exact conversion rate manually (offline)"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = formState.validationError?.contains("rate", ignoreCase = true) == true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Conversion Preview Card
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.spaceMedium),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Conversion Preview (Banker's Rounding)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = formState.destinationPreview?.let { destMoney ->
                                "Destination receives: ${destMoney.format()}"
                            } ?: "Enter source amount and exchange rate to preview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else if (sourceAccount != null && destAccount != null && sourceAccount.id != destAccount.id) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Same-currency transfer (${sourceAccount.currency.code}): total balance across both accounts is strictly conserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Date Selector
            DateSelectorField(
                dateEpochMs = formState.dateEpochMs,
                onDateSelected = onDateSelected,
                label = "Transfer Date"
            )

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = onNotesChanged,
                label = { Text("Notes (optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Validation Error Banner
            if (formState.validationError != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formState.validationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Submit Button
            Button(
                onClick = onSubmit,
                enabled = !formState.isSubmitting && accounts.size >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.minTouchTarget)
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Complete Transfer")
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        }
    }
}
