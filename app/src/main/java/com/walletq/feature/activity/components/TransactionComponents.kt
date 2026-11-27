package com.walletq.feature.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.ExpenseRed
import com.walletq.core.designsystem.IncomeGreen
import com.walletq.core.designsystem.VibrantCoral
import com.walletq.core.designsystem.VibrantElectricBlue
import com.walletq.core.designsystem.VibrantEmerald
import com.walletq.core.designsystem.WalletQMonetaryTypography
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.WarningAmber
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.designsystem.component.WalletQStatusBadge
import com.walletq.core.designsystem.component.WalletQStatusTone
import com.walletq.core.model.Account
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import com.walletq.feature.activity.DateRangeFilter
import com.walletq.feature.activity.OwnershipFilter
import com.walletq.feature.activity.TransactionFilterState
import com.walletq.feature.activity.categoryIconFor
import com.walletq.feature.activity.parseCategoryColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionListItem(
    transaction: Transaction,
    sourceAccount: Account?,
    destinationAccount: Account?,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = WalletQThemeTokens.colors
    val isReversed = transaction.status == TransactionStatus.REVERSED
    val contentAlpha = if (isReversed) 0.58f else 1f

    val iconVector = when (transaction.type) {
        TransactionType.TRANSFER -> Icons.Outlined.SwapHoriz
        else -> categoryIconFor(category?.iconIdentifier ?: "")
    }
    val defaultTint = when (transaction.type) {
        TransactionType.INCOME -> VibrantEmerald
        TransactionType.EXPENSE -> VibrantCoral
        TransactionType.TRANSFER -> VibrantElectricBlue
    }
    val iconTint = if (transaction.type == TransactionType.TRANSFER || category == null) {
        defaultTint
    } else {
        parseCategoryColor(category.colorIdentifier, defaultTint)
    }

    val amountColor = when {
        isReversed -> tokens.secondaryText
        transaction.type == TransactionType.INCOME -> tokens.success
        transaction.type == TransactionType.EXPENSE -> tokens.error
        else -> VibrantElectricBlue
    }

    val amountPrefix = when (transaction.type) {
        TransactionType.INCOME -> "+ "
        TransactionType.EXPENSE -> "- "
        TransactionType.TRANSFER -> ""
    }

    val timeLabel = remember(transaction.transactionDateEpochMs) {
        Instant.ofEpochMilli(transaction.transactionDateEpochMs)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
    }

    val subtitle = buildString {
        if (transaction.type == TransactionType.TRANSFER) {
            append(sourceAccount?.name ?: "Unknown")
            append(" → ")
            append(destinationAccount?.name ?: "Unknown")
        } else {
            append(sourceAccount?.name ?: "Unknown Account")
            if (category != null) {
                append(" • ")
                append(category.name)
            }
        }
        append(" • ")
        append(timeLabel)
    }

    WalletQCard(
        onClick = onClick,
        modifier = modifier.alpha(contentAlpha)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.iconBadgeContainer)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = category?.name ?: transaction.type.name,
                    tint = iconTint,
                    modifier = Modifier.size(Dimensions.iconMedium)
                )
            }

            Spacer(modifier = Modifier.width(Dimensions.spaceMediumSmall))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isReversed) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isReversed) {
                        WalletQStatusBadge(
                            text = "REVERSED",
                            tone = WalletQStatusTone.WARNING
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (transaction.ownershipType == OwnershipType.HOUSEHOLD) {
                        WalletQStatusBadge(
                            text = "Household",
                            tone = WalletQStatusTone.INFO
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Dimensions.spaceSmall))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${transaction.amount.format()}",
                    style = WalletQMonetaryTypography.medium,
                    color = amountColor,
                    textDecoration = if (isReversed) TextDecoration.LineThrough else TextDecoration.None
                )
                val destAmount = transaction.transferDetails?.destinationAmount
                if (transaction.type == TransactionType.TRANSFER &&
                    destAmount != null &&
                    destAmount.currency.code != transaction.amount.currency.code
                ) {
                    Text(
                        text = "→ ${destAmount.format()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterBar(
    filterState: TransactionFilterState,
    accounts: List<Account>,
    onSearchQueryChanged: (String) -> Unit,
    onOwnershipFilterChanged: (OwnershipFilter) -> Unit,
    onTypeFilterChanged: (TransactionType?) -> Unit,
    onAccountFilterChanged: (String?) -> Unit,
    onDateRangeFilterChanged: (DateRangeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var accountMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
    ) {
        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search description, notes, account...") },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (filterState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        // Row 1: Ownership + Type Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            OwnershipFilter.entries.forEach { ownership ->
                FilterChip(
                    selected = filterState.ownershipFilter == ownership,
                    onClick = { onOwnershipFilterChanged(ownership) },
                    label = { Text(ownership.label) }
                )
            }

            TransactionType.entries.forEach { type ->
                val selected = filterState.selectedType == type
                val label = when (type) {
                    TransactionType.INCOME -> "Income"
                    TransactionType.EXPENSE -> "Expense"
                    TransactionType.TRANSFER -> "Transfer"
                }
                FilterChip(
                    selected = selected,
                    onClick = { onTypeFilterChanged(if (selected) null else type) },
                    label = { Text(label) }
                )
            }
        }

        // Row 2: Date Range + Account Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateRangeFilter.entries.forEach { range ->
                FilterChip(
                    selected = filterState.dateRangeFilter == range,
                    onClick = { onDateRangeFilterChanged(range) },
                    label = { Text(range.label) }
                )
            }

            if (accounts.isNotEmpty()) {
                val selectedAccName = accounts.find { it.id == filterState.selectedAccountId }?.name
                ExposedDropdownMenuBox(
                    expanded = accountMenuExpanded,
                    onExpandedChange = { accountMenuExpanded = it }
                ) {
                    FilterChip(
                        selected = filterState.selectedAccountId != null,
                        onClick = { accountMenuExpanded = true },
                        label = { Text(selectedAccName ?: "All Accounts") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Accounts") },
                            onClick = {
                                onAccountFilterChanged(null)
                                accountMenuExpanded = false
                            }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${account.currency.code})") },
                                onClick = {
                                    onAccountFilterChanged(account.id)
                                    accountMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyAmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: Currency?,
    label: String = "Amount",
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val decimalHint = if (currency != null) {
        if (currency.decimalPlaces == 0) "${currency.code} (whole units)"
        else "${currency.code} (up to ${currency.decimalPlaces} decimals)"
    } else {
        "Select an account first"
    }

    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            // Allow only digits and at most one decimal point
            if (next.all { it.isDigit() || it == '.' } && next.count { it == '.' } <= 1) {
                onValueChange(next)
            }
        },
        label = { Text(label) },
        prefix = currency?.let { { Text("${it.symbol} ") } },
        suffix = currency?.let { { Text(it.code) } },
        supportingText = { Text(decimalHint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = isError,
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectorDropdown(
    label: String,
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (accounts.isNotEmpty()) expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedAccount?.let { "${it.name} (${it.currency.code})" }
                ?: if (accounts.isEmpty()) "No accounts available" else "Select account",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = if (selectedAccount != null) {
                {
                    val scopeLabel = if (selectedAccount.ownershipType == OwnershipType.HOUSEHOLD) "Household" else "Personal"
                    val bal = com.walletq.core.model.Money(selectedAccount.currentBalanceMinorUnits, selectedAccount.currency).format()
                    Text("$scopeLabel • Balance: $bal")
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                val scopeLabel = if (account.ownershipType == OwnershipType.HOUSEHOLD) "Household" else "Personal"
                val bal = com.walletq.core.model.Money(account.currentBalanceMinorUnits, account.currency).format()
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "${account.name} (${account.currency.code})",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "$scopeLabel • Balance: $bal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onAccountSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelectorSection(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        if (categories.isEmpty()) {
            Text(
                text = "No matching categories available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
            ) {
                categories.forEach { category ->
                    val selected = category.id == selectedCategoryId
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIconFor(category.iconIdentifier),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorField(
    dateEpochMs: Long,
    onDateSelected: (Long) -> Unit,
    label: String = "Date",
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(dateEpochMs) {
        Instant.ofEpochMilli(dateEpochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }

    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = "Select date")
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateEpochMs)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDateSelected)
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
