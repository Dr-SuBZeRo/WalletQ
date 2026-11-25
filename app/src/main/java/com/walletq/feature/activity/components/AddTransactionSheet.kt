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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.model.Account
import com.walletq.core.model.Category
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.TransactionType
import com.walletq.feature.activity.TransactionFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    formState: TransactionFormState,
    accounts: List<Account>,
    filteredCategories: List<Category>,
    onDismiss: () -> Unit,
    onTypeChanged: (TransactionType) -> Unit,
    onOwnershipContextChanged: (OwnershipType) -> Unit,
    onAccountSelected: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedAccount = accounts.find { it.id == formState.selectedAccountId }
    val scopedAccounts = accounts.filter { it.ownershipType == formState.ownershipContext }
        .ifEmpty { accounts }

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
                text = if (formState.transactionType == TransactionType.INCOME) "Record Income" else "Record Expense",
                style = MaterialTheme.typography.headlineSmall
            )

            // Type Toggle: Expense / Income
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = formState.transactionType == type,
                        onClick = { onTypeChanged(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                    ) {
                        Text(if (type == TransactionType.EXPENSE) "Expense" else "Income")
                    }
                }
            }

            // Ownership Context
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                OwnershipType.entries.forEach { ownership ->
                    val hasMatchingAccounts = accounts.any { it.ownershipType == ownership }
                    FilterChip(
                        selected = formState.ownershipContext == ownership,
                        onClick = { onOwnershipContextChanged(ownership) },
                        enabled = hasMatchingAccounts || accounts.isEmpty(),
                        label = {
                            Text(if (ownership == OwnershipType.PERSONAL) "Personal" else "Household")
                        }
                    )
                }
            }

            if (accounts.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No active accounts found. Create an account in the Accounts tab before recording transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Dimensions.spaceMedium)
                    )
                }
            }

            // Account Selector
            AccountSelectorDropdown(
                label = "Account",
                accounts = scopedAccounts,
                selectedAccountId = formState.selectedAccountId,
                onAccountSelected = onAccountSelected
            )

            // Amount Input
            CurrencyAmountTextField(
                value = formState.amountInput,
                onValueChange = onAmountChanged,
                currency = selectedAccount?.currency,
                isError = formState.validationError?.contains("Amount", ignoreCase = true) == true
            )

            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = onDescriptionChanged,
                label = { Text("Description") },
                placeholder = { Text("e.g., Groceries, Monthly Salary") },
                singleLine = true,
                isError = formState.validationError?.contains("description", ignoreCase = true) == true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selector
            CategorySelectorSection(
                categories = filteredCategories,
                selectedCategoryId = formState.selectedCategoryId,
                onCategorySelected = onCategorySelected
            )

            // Date Selector
            DateSelectorField(
                dateEpochMs = formState.dateEpochMs,
                onDateSelected = onDateSelected
            )

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = onNotesChanged,
                label = { Text("Notes (optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Validation / Repository Error Banner
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
                enabled = !formState.isSubmitting && accounts.isNotEmpty(),
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
                    Text(
                        text = if (formState.transactionType == TransactionType.INCOME) "Save Income" else "Save Expense"
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        }
    }
}
