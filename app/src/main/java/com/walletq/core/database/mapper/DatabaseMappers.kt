package com.walletq.core.database.mapper

import com.walletq.core.database.dao.BudgetWithAllocations
import com.walletq.core.database.entity.AccountEntity
import com.walletq.core.database.entity.BudgetCategoryAllocationEntity
import com.walletq.core.database.entity.BudgetEntity
import com.walletq.core.database.entity.CategoryEntity
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.LedgerEntryEntity
import com.walletq.core.database.entity.TransactionEntity
import com.walletq.core.model.Account
import com.walletq.core.model.Budget
import com.walletq.core.model.BudgetCategoryAllocation
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.HouseholdMember
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionType
import com.walletq.core.model.TransferDetails
import java.math.BigDecimal

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    profileId = profileId,
    householdId = householdId,
    ownershipType = ownershipType,
    name = name,
    type = type,
    currency = Currency.findByCode(currencyCode),
    initialBalanceMinorUnits = initialBalanceMinorUnits,
    currentBalanceMinorUnits = currentBalanceMinorUnits,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    openingBalanceDateEpochMs = openingBalanceDateEpochMs,
    description = description,
    iconIdentifier = iconIdentifier,
    colorIdentifier = colorIdentifier
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    profileId = profileId,
    householdId = householdId,
    ownershipType = ownershipType,
    name = name,
    type = type,
    currencyCode = currency.code,
    initialBalanceMinorUnits = initialBalanceMinorUnits,
    currentBalanceMinorUnits = currentBalanceMinorUnits,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    openingBalanceDateEpochMs = openingBalanceDateEpochMs,
    description = description,
    iconIdentifier = iconIdentifier,
    colorIdentifier = colorIdentifier
)

fun HouseholdEntity.toDomain(): Household = Household(
    id = id,
    name = name,
    description = description,
    createdAtEpochMs = createdAtEpochMs
)

fun HouseholdMemberEntity.toDomain(): HouseholdMember = HouseholdMember(
    householdId = householdId,
    profileId = profileId,
    role = role,
    joinedAtEpochMs = joinedAtEpochMs
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    transactionType = transactionType,
    iconIdentifier = iconIdentifier,
    colorIdentifier = colorIdentifier,
    parentCategoryId = parentCategoryId,
    profileId = profileId,
    householdId = householdId,
    isSystemCategory = isSystemCategory,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    transactionType = transactionType,
    iconIdentifier = iconIdentifier,
    colorIdentifier = colorIdentifier,
    parentCategoryId = parentCategoryId,
    profileId = profileId,
    householdId = householdId,
    isSystemCategory = isSystemCategory,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs
)

fun TransactionEntity.toDomain(): Transaction {
    val cur = Currency.findByCode(currencyCode)
    val destCur = destinationCurrencyCode?.let { Currency.findByCode(it) }
    val transfer = if (transactionType == TransactionType.TRANSFER &&
        destinationAccountId != null &&
        destinationAmountMinorUnits != null &&
        destCur != null
    ) {
        TransferDetails(
            destinationAccountId = destinationAccountId,
            destinationAmount = Money(destinationAmountMinorUnits, destCur),
            appliedExchangeRate = exchangeRate?.let { BigDecimal(it) },
            exchangeRateReferenceId = exchangeRateReferenceId
        )
    } else null

    return Transaction(
        id = id,
        type = transactionType,
        amount = Money(amountMinorUnits, cur),
        accountId = sourceAccountId,
        categoryId = categoryId,
        transactionDateEpochMs = transactionDateEpochMs,
        description = description,
        notes = notes,
        ownershipType = if (householdId != null) OwnershipType.HOUSEHOLD else OwnershipType.PERSONAL,
        ownerProfileId = profileId ?: "",
        householdId = householdId,
        transferDetails = transfer,
        status = transactionStatus,
        originalTransactionId = originalTransactionId,
        reversalTransactionId = reversalTransactionId,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    transactionType = type,
    transactionStatus = status,
    amountMinorUnits = amount.minorUnits,
    currencyCode = amount.currency.code,
    sourceAccountId = accountId,
    destinationAccountId = transferDetails?.destinationAccountId,
    categoryId = categoryId,
    profileId = if (ownershipType == OwnershipType.PERSONAL) ownerProfileId else (ownerProfileId.ifBlank { null }),
    householdId = householdId,
    description = description,
    notes = notes,
    transactionDateEpochMs = transactionDateEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    originalTransactionId = originalTransactionId,
    reversalTransactionId = reversalTransactionId,
    exchangeRate = transferDetails?.appliedExchangeRate?.toPlainString(),
    exchangeRateReferenceId = transferDetails?.exchangeRateReferenceId,
    destinationAmountMinorUnits = transferDetails?.destinationAmount?.minorUnits,
    destinationCurrencyCode = transferDetails?.destinationAmount?.currency?.code
)

fun LedgerEntryEntity.toDomain(): LedgerEntry = LedgerEntry(
    id = id,
    transactionId = transactionId,
    accountId = accountId,
    signedMinorUnits = signedMinorUnits,
    currency = Currency.findByCode(currencyCode),
    entryType = entryType,
    timestampEpochMs = timestampEpochMs
)

fun LedgerEntry.toEntity(): LedgerEntryEntity = LedgerEntryEntity(
    id = id,
    transactionId = transactionId,
    accountId = accountId,
    signedMinorUnits = signedMinorUnits,
    currencyCode = currency.code,
    entryType = entryType,
    timestampEpochMs = timestampEpochMs
)

fun BudgetCategoryAllocationEntity.toDomain(): BudgetCategoryAllocation = BudgetCategoryAllocation(
    id = id,
    budgetId = budgetId,
    categoryId = categoryId,
    allocatedAmountMinorUnits = allocatedAmountMinorUnits
)

fun BudgetCategoryAllocation.toEntity(): BudgetCategoryAllocationEntity = BudgetCategoryAllocationEntity(
    id = id,
    budgetId = budgetId,
    categoryId = categoryId,
    allocatedAmountMinorUnits = allocatedAmountMinorUnits
)

fun BudgetWithAllocations.toDomain(): Budget = Budget(
    id = budget.id,
    name = budget.name,
    period = budget.period,
    startDateEpochMs = budget.startDateEpochMs,
    endDateEpochMs = budget.endDateEpochMs,
    currency = Currency.findByCode(budget.currencyCode),
    allocatedAmountMinorUnits = budget.allocatedAmountMinorUnits,
    ownershipType = budget.ownershipType,
    profileId = budget.profileId,
    householdId = budget.householdId,
    isArchived = budget.isArchived,
    createdAtEpochMs = budget.createdAtEpochMs,
    updatedAtEpochMs = budget.updatedAtEpochMs,
    categoryAllocations = allocations.map { it.toDomain() }
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    period = period,
    startDateEpochMs = startDateEpochMs,
    endDateEpochMs = endDateEpochMs,
    currencyCode = currency.code,
    allocatedAmountMinorUnits = allocatedAmountMinorUnits,
    ownershipType = ownershipType,
    profileId = profileId,
    householdId = householdId,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)
