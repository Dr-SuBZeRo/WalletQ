package com.walletq.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.OwnershipType
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditBudgetSheet(
    formState: BudgetFormState,
    expenseCategories: List<Category>,
    categoriesById: Map<String, Category>,
    households: List<Household>,
    onNameChanged: (String) -> Unit,
    onYearMonthChanged: (YearMonth) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onAllocatedAmountChanged: (String) -> Unit,
    onOwnershipChanged: (OwnershipType) -> Unit,
    onHouseholdSelected: (String) -> Unit,
    onAddCategoryAllocation: (String) -> Unit,
    onCategoryAmountChanged: (String, String) -> Unit,
    onRemoveCategoryAllocation: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedCurrency = remember(formState.currencyCode) {
        Currency.findByCode(formState.currencyCode)
    }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

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
            Text(
                text = if (formState.isEditing) "Edit Budget" else "Create Monthly Budget",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = formState.name,
                onValueChange = onNameChanged,
                label = { Text("Budget Name") },
                placeholder = { Text("e.g., Monthly Living, Household Groceries") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Period",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onYearMonthChanged(formState.yearMonth.minusMonths(1)) }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = formState.yearMonth.format(monthFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { onYearMonthChanged(formState.yearMonth.plusMonths(1)) }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
                    }
                }
            }

            // Currency Selector
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)) {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                    Currency.SUPPORTED_CURRENCIES.forEach { currency ->
                        FilterChip(
                            selected = formState.currencyCode == currency.code,
                            onClick = { onCurrencyChanged(currency.code) },
                            label = { Text("${currency.code} (${currency.symbol})") }
                        )
                    }
                }
            }

            // Allocated Amount
            OutlinedTextField(
                value = formState.allocatedAmountInput,
                onValueChange = onAllocatedAmountChanged,
                label = { Text("Monthly Budget Limit") },
                prefix = { Text("${selectedCurrency.symbol} ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Ownership
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)) {
                Text(
                    text = "Ownership",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    OwnershipType.entries.forEachIndexed { index, ownership ->
                        val label = if (ownership == OwnershipType.PERSONAL) "Personal" else "Household"
                        SegmentedButton(
                            selected = formState.ownershipType == ownership,
                            onClick = { onOwnershipChanged(ownership) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = OwnershipType.entries.size
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            if (formState.ownershipType == OwnershipType.HOUSEHOLD && households.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                    households.forEach { household ->
                        FilterChip(
                            selected = formState.selectedHouseholdId == household.id,
                            onClick = { onHouseholdSelected(household.id) },
                            label = { Text(household.name) }
                        )
                    }
                }
            }

            // Optional Expense Category Allocations
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                Text(
                    text = "Expense Category Limits (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val allocatedIds = formState.categoryAllocations.map { it.categoryId }.toSet()
                val availableCategories = expenseCategories.filter { it.id !in allocatedIds }

                if (availableCategories.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                        availableCategories.forEach { category ->
                            FilterChip(
                                selected = false,
                                onClick = { onAddCategoryAllocation(category.id) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(category.name) }
                            )
                        }
                    }
                }

                formState.categoryAllocations.forEach { draft ->
                    val catName = categoriesById[draft.categoryId]?.name ?: draft.categoryId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                    ) {
                        OutlinedTextField(
                            value = draft.amountInput,
                            onValueChange = { onCategoryAmountChanged(draft.id, it) },
                            label = { Text(catName) },
                            prefix = { Text("${selectedCurrency.symbol} ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveCategoryAllocation(draft.id) }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove $catName"
                            )
                        }
                    }
                }
            }

            if (formState.validationError != null) {
                Text(
                    text = formState.validationError,
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
                    enabled = !formState.isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (formState.isEditing) "Save Budget" else "Create Budget")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        }
    }
}
