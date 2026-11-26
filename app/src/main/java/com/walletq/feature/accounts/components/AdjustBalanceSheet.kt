package com.walletq.feature.accounts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.model.Money
import com.walletq.feature.accounts.BalanceAdjustmentFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustBalanceSheet(
    state: BalanceAdjustmentFormState,
    onTargetBalanceChanged: (String) -> Unit,
    onReasonChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentMoney = Money(state.currentBalanceMinorUnits, state.currency)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.spaceMedium)
                .padding(bottom = Dimensions.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            Text(
                text = "Adjust Balance — ${state.accountName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Current ledger balance: ${currentMoney.format()}. " +
                    "This records an auditable adjustment transaction in the ledger without overwriting historical records.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = state.targetBalanceInput,
                onValueChange = onTargetBalanceChanged,
                label = { Text("New Target Balance") },
                prefix = { Text("${state.currency.symbol} ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.reason,
                onValueChange = onReasonChanged,
                label = { Text("Reason (Optional)") },
                placeholder = { Text("e.g., Bank statement reconciliation") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.validationError != null) {
                Text(
                    text = state.validationError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSubmit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Record Adjustment")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        }
    }
}
