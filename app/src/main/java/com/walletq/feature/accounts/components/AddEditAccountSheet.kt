package com.walletq.feature.accounts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.model.AccountType
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.OwnershipType
import com.walletq.feature.accounts.AccountFormState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAccountSheet(
    formState: AccountFormState,
    households: List<Household>,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (AccountType) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onOwnershipChanged: (OwnershipType) -> Unit,
    onHouseholdSelected: (String) -> Unit,
    onOpeningBalanceChanged: (String) -> Unit,
    onOpeningBalanceDateChanged: (Long) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedCurrency = remember(formState.currencyCode) {
        Currency.findByCode(formState.currencyCode)
    }

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
                text = if (formState.isEditing) "Edit Account Details" else "Add Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = formState.name,
                onValueChange = onNameChanged,
                label = { Text("Account Name") },
                placeholder = { Text("e.g., Primary Checking, Cash Wallet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Account Type
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)) {
                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val types = remember(formState.type) {
                    if (formState.type == AccountType.INVESTMENT) {
                        AccountVisuals.selectableAccountTypes + AccountType.INVESTMENT
                    } else {
                        AccountVisuals.selectableAccountTypes
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
                ) {
                    types.forEach { type ->
                        FilterChip(
                            selected = formState.type == type,
                            onClick = { onTypeChanged(type) },
                            label = { Text(type.displayName) }
                        )
                    }
                }
            }

            // Currency
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)) {
                Text(
                    text = if (formState.isEditing) "Currency (locked)" else "Currency",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
                ) {
                    Currency.SUPPORTED_CURRENCIES.forEach { currency ->
                        FilterChip(
                            selected = formState.currencyCode == currency.code,
                            onClick = { if (!formState.isEditing) onCurrencyChanged(currency.code) },
                            enabled = !formState.isEditing || formState.currencyCode == currency.code,
                            label = { Text("${currency.code} (${currency.symbol})") }
                        )
                    }
                }
            }

            // Ownership
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)) {
                Text(
                    text = if (formState.isEditing) "Ownership (locked)" else "Ownership",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    OwnershipType.entries.forEachIndexed { index, ownership ->
                        val label = if (ownership == OwnershipType.PERSONAL) "Personal" else "Household"
                        SegmentedButton(
                            selected = formState.ownershipType == ownership,
                            onClick = { if (!formState.isEditing) onOwnershipChanged(ownership) },
                            enabled = !formState.isEditing || formState.ownershipType == ownership,
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

            // Household selection when HOUSEHOLD
            if (formState.ownershipType == OwnershipType.HOUSEHOLD) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)) {
                    Text(
                        text = "Household",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (households.isEmpty()) {
                        Text(
                            text = "No households available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                        ) {
                            households.forEach { household ->
                                FilterChip(
                                    selected = formState.selectedHouseholdId == household.id,
                                    onClick = {
                                        if (!formState.isEditing) onHouseholdSelected(household.id)
                                    },
                                    enabled = !formState.isEditing || formState.selectedHouseholdId == household.id,
                                    label = { Text(household.name) }
                                )
                            }
                        }
                    }
                }
            }

            // Opening Balance & Date
            val precisionHint = if (selectedCurrency.decimalPlaces == 0) {
                "Whole units only (${selectedCurrency.code} has 0 decimals)"
            } else {
                "Up to ${selectedCurrency.decimalPlaces} decimal places (${selectedCurrency.code})"
            }

            OutlinedTextField(
                value = formState.openingBalanceInput,
                onValueChange = { if (!formState.isEditing) onOpeningBalanceChanged(it) },
                enabled = !formState.isEditing,
                label = {
                    Text(if (formState.isEditing) "Opening Balance (use Adjust Balance to correct)" else "Opening Balance")
                },
                prefix = { Text("${selectedCurrency.symbol} ") },
                supportingText = { Text(precisionHint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { if (!formState.isEditing) showDatePicker = true },
                enabled = !formState.isEditing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(Dimensions.spaceSmall))
                Text("Opening Balance Date: ${AccountVisuals.formatDate(formState.openingBalanceDateEpochMs)}")
            }

            // Optional Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = onDescriptionChanged,
                label = { Text("Description (Optional)") },
                placeholder = { Text("Institution, last 4 digits, or purpose") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Optional Color
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)) {
                Text(
                    text = "Color Accent (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                    AccountVisuals.colorOptions.forEach { option ->
                        val isSelected = formState.colorIdentifier == option.id
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { onColorSelected(option.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = option.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Optional Icon
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)) {
                Text(
                    text = "Icon (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    AccountVisuals.iconOptions.forEach { option ->
                        FilterChip(
                            selected = formState.iconIdentifier == option.id,
                            onClick = { onIconSelected(option.id) },
                            leadingIcon = {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(option.label) }
                        )
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
                        Text(if (formState.isEditing) "Save Changes" else "Create Account")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.openingBalanceDateEpochMs
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onOpeningBalanceDateChanged)
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
