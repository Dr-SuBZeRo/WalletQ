package com.walletq.core.database.repository

import com.walletq.core.database.TransactionRunner
import com.walletq.core.database.dao.BudgetDao
import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.mapper.toDomain
import com.walletq.core.database.mapper.toEntity
import com.walletq.core.model.Budget
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val currencyDao: CurrencyDao,
    private val profileDao: ProfileDao,
    private val householdDao: HouseholdDao
) : BudgetRepository {

    override fun observeAllBudgets(): Flow<List<Budget>> {
        return budgetDao.observeAllBudgetsWithAllocations().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAccessibleBudgets(profileId: String): Flow<List<Budget>> {
        return combine(
            budgetDao.observeAllBudgetsWithAllocations(),
            householdDao.getHouseholdsForProfileFlow(profileId)
        ) { budgets, memberHouseholds ->
            val memberHouseholdIds = memberHouseholds.map { it.id }.toSet()
            budgets.map { it.toDomain() }.filter { budget ->
                when (budget.ownershipType) {
                    OwnershipType.PERSONAL -> budget.profileId == profileId
                    OwnershipType.HOUSEHOLD -> budget.householdId != null && budget.householdId in memberHouseholdIds
                }
            }
        }
    }

    override fun observeBudgetById(budgetId: String): Flow<Budget?> {
        return budgetDao.observeBudgetWithAllocationsById(budgetId).map { it?.toDomain() }
    }

    override suspend fun getBudgetById(
        budgetId: String,
        requesterProfileId: String
    ): Result<Budget> = runCatching {
        val budget = budgetDao.getBudgetWithAllocationsById(budgetId)?.toDomain()
            ?: throw IllegalArgumentException("Budget '$budgetId' does not exist.")
        verifyAccessAuthorization(budget, requesterProfileId)
        budget
    }

    override suspend fun createBudget(
        budget: Budget,
        requesterProfileId: String
    ): Result<Budget> = runCatching {
        transactionRunner {
            val normalized = validateAndNormalizeBudget(budget, requesterProfileId)
            budgetDao.insertBudget(normalized.toEntity())
            if (normalized.categoryAllocations.isNotEmpty()) {
                budgetDao.insertAllocations(normalized.categoryAllocations.map { it.toEntity() })
            }
            normalized
        }
    }

    override suspend fun updateBudget(
        budget: Budget,
        requesterProfileId: String
    ): Result<Budget> = runCatching {
        transactionRunner {
            val existing = budgetDao.getBudgetWithAllocationsById(budget.id)?.toDomain()
                ?: throw IllegalArgumentException("Budget '${budget.id}' does not exist.")
            verifyAccessAuthorization(existing, requesterProfileId)

            val normalized = validateAndNormalizeBudget(
                budget.copy(createdAtEpochMs = existing.createdAtEpochMs),
                requesterProfileId
            )
            budgetDao.updateBudget(normalized.toEntity())
            budgetDao.deleteAllocationsForBudget(normalized.id)
            if (normalized.categoryAllocations.isNotEmpty()) {
                budgetDao.insertAllocations(normalized.categoryAllocations.map { it.toEntity() })
            }
            normalized
        }
    }

    override suspend fun archiveBudget(
        budgetId: String,
        requesterProfileId: String
    ): Result<Unit> = runCatching {
        transactionRunner {
            val existing = budgetDao.getBudgetWithAllocationsById(budgetId)?.toDomain()
                ?: throw IllegalArgumentException("Budget '$budgetId' does not exist.")
            verifyAccessAuthorization(existing, requesterProfileId)
            check(!existing.isArchived) { "Budget '${existing.name}' is already archived." }
            budgetDao.setArchived(budgetId, true, System.currentTimeMillis())
        }
    }

    override suspend fun unarchiveBudget(
        budgetId: String,
        requesterProfileId: String
    ): Result<Unit> = runCatching {
        transactionRunner {
            val existing = budgetDao.getBudgetWithAllocationsById(budgetId)?.toDomain()
                ?: throw IllegalArgumentException("Budget '$budgetId' does not exist.")
            verifyAccessAuthorization(existing, requesterProfileId)
            check(existing.isArchived) { "Budget '${existing.name}' is already active." }
            budgetDao.setArchived(budgetId, false, System.currentTimeMillis())
        }
    }

    private suspend fun verifyAccessAuthorization(
        budget: Budget,
        requesterProfileId: String
    ) {
        when (budget.ownershipType) {
            OwnershipType.PERSONAL -> {
                require(budget.profileId == requesterProfileId) {
                    "Profile '$requesterProfileId' is not authorized to access this personal budget."
                }
            }
            OwnershipType.HOUSEHOLD -> {
                val householdId = requireNotNull(budget.householdId) {
                    "Household budget must specify a householdId."
                }
                val members = householdDao.getMembersForHousehold(householdId)
                require(members.any { it.profileId == requesterProfileId }) {
                    "Profile '$requesterProfileId' is not an authorized member of household '$householdId'."
                }
            }
        }
    }

    private suspend fun validateAndNormalizeBudget(
        budget: Budget,
        requesterProfileId: String
    ): Budget {
        val trimmedName = budget.name.trim()
        require(trimmedName.isNotEmpty()) { "Budget name cannot be blank." }
        require(budget.startDateEpochMs <= budget.endDateEpochMs) {
            "Budget start date must be on or before end date."
        }
        require(budget.allocatedAmountMinorUnits > 0L) {
            "Budget allocated amount must be strictly positive (> 0)."
        }

        currencyDao.getByCode(budget.currency.code)
            ?: throw IllegalArgumentException("Currency '${budget.currency.code}' is not supported.")

        when (budget.ownershipType) {
            OwnershipType.PERSONAL -> {
                val profileId = requireNotNull(budget.profileId) {
                    "Personal budget must specify a profileId."
                }
                require(budget.householdId == null) {
                    "Personal budget must not specify a householdId."
                }
                require(profileId == requesterProfileId) {
                    "Profile '$requesterProfileId' cannot create or modify a budget for another profile."
                }
                profileDao.getById(profileId)
                    ?: throw IllegalArgumentException("Profile '$profileId' does not exist.")
            }
            OwnershipType.HOUSEHOLD -> {
                val householdId = requireNotNull(budget.householdId) {
                    "Household budget must specify a householdId."
                }
                householdDao.getById(householdId)
                    ?: throw IllegalArgumentException("Household '$householdId' does not exist.")
                val members = householdDao.getMembersForHousehold(householdId)
                require(members.any { it.profileId == requesterProfileId }) {
                    "Profile '$requesterProfileId' is not an authorized member of household '$householdId'."
                }
            }
        }

        // Validate category allocations: no duplicates, positive amounts, EXPENSE-only, matching ownership
        val seenCategoryIds = mutableSetOf<String>()
        var sumOfAllocations = 0L
        val normalizedAllocations = budget.categoryAllocations.map { alloc ->
            require(seenCategoryIds.add(alloc.categoryId)) {
                "Duplicate category allocation '${alloc.categoryId}' within the same budget is not permitted."
            }
            require(alloc.allocatedAmountMinorUnits > 0L) {
                "Category allocation amount must be strictly positive (> 0)."
            }
            val category = categoryDao.getById(alloc.categoryId)
                ?: throw IllegalArgumentException("Category '${alloc.categoryId}' does not exist.")
            require(category.transactionType == TransactionType.EXPENSE) {
                "Only EXPENSE categories can be allocated in a budget. '${category.name}' is ${category.transactionType}."
            }
            if (!category.isSystemCategory) {
                when (budget.ownershipType) {
                    OwnershipType.PERSONAL -> require(category.profileId == budget.profileId) {
                        "Category '${category.name}' does not belong to this personal profile."
                    }
                    OwnershipType.HOUSEHOLD -> require(category.householdId == budget.householdId) {
                        "Category '${category.name}' does not belong to this household."
                    }
                }
            }
            sumOfAllocations = Math.addExact(sumOfAllocations, alloc.allocatedAmountMinorUnits)
            alloc.copy(budgetId = budget.id)
        }

        return budget.copy(
            name = trimmedName,
            categoryAllocations = normalizedAllocations
        )
    }
}
