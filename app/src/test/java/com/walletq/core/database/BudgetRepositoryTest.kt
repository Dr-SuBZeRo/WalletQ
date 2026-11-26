package com.walletq.core.database

import com.walletq.core.database.dao.BudgetDao
import com.walletq.core.database.dao.BudgetWithAllocations
import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.entity.BudgetCategoryAllocationEntity
import com.walletq.core.database.entity.BudgetEntity
import com.walletq.core.database.entity.CategoryEntity
import com.walletq.core.database.entity.CurrencyEntity
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.database.repository.BudgetRepositoryImpl
import com.walletq.core.model.Budget
import com.walletq.core.model.BudgetCalculationEngine
import com.walletq.core.model.BudgetCategoryAllocation
import com.walletq.core.model.BudgetPeriod
import com.walletq.core.model.Currency
import com.walletq.core.model.HouseholdRole
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

class BudgetRepositoryTest {

    private class FakeBudgetDao : BudgetDao {
        val budgets = MutableStateFlow<Map<String, BudgetEntity>>(emptyMap())
        val allocations = MutableStateFlow<Map<String, List<BudgetCategoryAllocationEntity>>>(emptyMap())

        override suspend fun insertBudget(budget: BudgetEntity) {
            budgets.value = budgets.value + (budget.id to budget)
        }

        override suspend fun updateBudget(budget: BudgetEntity) {
            budgets.value = budgets.value + (budget.id to budget)
        }

        override suspend fun insertAllocations(allocations: List<BudgetCategoryAllocationEntity>) {
            val grouped = allocations.groupBy { it.budgetId }
            val current = this.allocations.value.toMutableMap()
            grouped.forEach { (budgetId, list) ->
                current[budgetId] = (current[budgetId].orEmpty() + list)
            }
            this.allocations.value = current
        }

        override suspend fun deleteAllocationsForBudget(budgetId: String) {
            allocations.value = allocations.value - budgetId
        }

        override fun observeAllBudgetsWithAllocations(): Flow<List<BudgetWithAllocations>> =
            budgets.map { map ->
                map.values.map { b -> BudgetWithAllocations(b, allocations.value[b.id].orEmpty()) }
            }

        override suspend fun getBudgetWithAllocationsById(id: String): BudgetWithAllocations? {
            val b = budgets.value[id] ?: return null
            return BudgetWithAllocations(b, allocations.value[id].orEmpty())
        }

        override fun observeBudgetWithAllocationsById(id: String): Flow<BudgetWithAllocations?> =
            budgets.map { map -> map[id]?.let { BudgetWithAllocations(it, allocations.value[id].orEmpty()) } }

        override suspend fun setArchived(id: String, isArchived: Boolean, updatedAtEpochMs: Long) {
            val existing = budgets.value[id] ?: return
            budgets.value = budgets.value + (id to existing.copy(isArchived = isArchived, updatedAtEpochMs = updatedAtEpochMs))
        }

        override suspend fun getCount(): Int = budgets.value.size
    }

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableMapOf(
            "cat-food" to CategoryEntity("cat-food", "Food", TransactionType.EXPENSE, "restaurant", "#FF5722", null, null, null, true, false, 1000L),
            "cat-salary" to CategoryEntity("cat-salary", "Salary", TransactionType.INCOME, "payments", "#4CAF50", null, null, null, true, false, 1000L)
        )
        override suspend fun insert(category: CategoryEntity): Long { categories[category.id] = category; return 1L }
        override suspend fun insertAll(categories: List<CategoryEntity>) {}
        override suspend fun update(category: CategoryEntity) {}
        override suspend fun delete(category: CategoryEntity) {}
        override suspend fun getById(id: String): CategoryEntity? = categories[id]
        override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(categories.values.toList())
        override fun observeActive(): Flow<List<CategoryEntity>> = flowOf(categories.values.filter { !it.isArchived })
        override fun observeByType(type: TransactionType): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeSystemCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeByProfile(profileId: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeByHousehold(householdId: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun setArchived(id: String, isArchived: Boolean) {}
        override suspend fun getCount(): Int = categories.size
    }

    private class FakeCurrencyDao : CurrencyDao {
        override suspend fun insertAll(currencies: List<CurrencyEntity>) {}
        override suspend fun insert(currency: CurrencyEntity) {}
        override fun getAllCurrenciesFlow(): Flow<List<CurrencyEntity>> = flowOf(emptyList())
        override suspend fun getByCode(code: String): CurrencyEntity? {
            val c = Currency.SUPPORTED_CURRENCIES.find { it.code == code } ?: return null
            return CurrencyEntity(c.code, c.displayName, c.symbol, c.decimalPlaces, c.code == "INR")
        }
        override fun getDefaultReportingCurrencyFlow(): Flow<CurrencyEntity?> = flowOf(null)
    }

    private class FakeProfileDao : ProfileDao {
        val profiles = mapOf(
            "profile-1" to ProfileEntity("profile-1", "Personal", OwnershipType.PERSONAL, true, 1000L),
            "profile-2" to ProfileEntity("profile-2", "Outsider", OwnershipType.PERSONAL, false, 1000L)
        )
        override suspend fun insert(profile: ProfileEntity) {}
        override suspend fun update(profile: ProfileEntity) {}
        override suspend fun delete(profile: ProfileEntity) {}
        override suspend fun getById(id: String): ProfileEntity? = profiles[id]
        override fun getAllProfilesFlow(): Flow<List<ProfileEntity>> = flowOf(profiles.values.toList())
        override fun getDefaultProfileFlow(): Flow<ProfileEntity?> = flowOf(profiles["profile-1"])
        override suspend fun getCount(): Int = profiles.size
    }

    private class FakeHouseholdDao : HouseholdDao {
        val households = mapOf(
            "hh-1" to HouseholdEntity("hh-1", "Family", "Shared", 1000L),
            "hh-2" to HouseholdEntity("hh-2", "Other", "Unrelated", 1000L)
        )
        val members = listOf(
            HouseholdMemberEntity("hh-1", "profile-1", HouseholdRole.OWNER, 1000L),
            HouseholdMemberEntity("hh-2", "profile-2", HouseholdRole.OWNER, 1000L)
        )
        override suspend fun insert(household: HouseholdEntity) {}
        override suspend fun update(household: HouseholdEntity) {}
        override suspend fun delete(household: HouseholdEntity) {}
        override suspend fun insertMember(member: HouseholdMemberEntity) {}
        override suspend fun deleteMember(householdId: String, profileId: String) {}
        override suspend fun getById(id: String): HouseholdEntity? = households[id]
        override fun getAllHouseholdsFlow(): Flow<List<HouseholdEntity>> = flowOf(households.values.toList())
        override fun getMembersForHouseholdFlow(householdId: String): Flow<List<HouseholdMemberEntity>> =
            flowOf(members.filter { it.householdId == householdId })
        override suspend fun getMembersForHousehold(householdId: String): List<HouseholdMemberEntity> =
            members.filter { it.householdId == householdId }
        override fun getHouseholdsForProfileFlow(profileId: String): Flow<List<HouseholdEntity>> =
            flowOf(members.filter { it.profileId == profileId }.mapNotNull { households[it.householdId] })
        override suspend fun getMemberCount(householdId: String): Int = 1
        override suspend fun getHouseholdCount(): Int = households.size
    }

    private lateinit var repository: BudgetRepositoryImpl
    private val boundaries = BudgetCalculationEngine.monthBoundaries(YearMonth.of(2026, 11))

    @Before
    fun setup() {
        val runner = object : TransactionRunner {
            override suspend fun <T> invoke(block: suspend () -> T): T = block()
        }
        repository = BudgetRepositoryImpl(
            transactionRunner = runner,
            budgetDao = FakeBudgetDao(),
            categoryDao = FakeCategoryDao(),
            currencyDao = FakeCurrencyDao(),
            profileDao = FakeProfileDao(),
            householdDao = FakeHouseholdDao()
        )
    }

    private fun sampleBudget(
        id: String = "b-1",
        ownershipType: OwnershipType = OwnershipType.PERSONAL,
        profileId: String? = "profile-1",
        householdId: String? = null,
        allocations: List<BudgetCategoryAllocation> = emptyList()
    ) = Budget(
        id = id,
        name = "November Budget",
        period = BudgetPeriod.MONTHLY,
        startDateEpochMs = boundaries.first,
        endDateEpochMs = boundaries.second,
        currency = Currency.INR,
        allocatedAmountMinorUnits = 100000L,
        ownershipType = ownershipType,
        profileId = profileId,
        householdId = householdId,
        createdAtEpochMs = 1000L,
        updatedAtEpochMs = 1000L,
        categoryAllocations = allocations
    )

    @Test
    fun createAndEditBudget_succeedsWithExpenseCategories() = runTest {
        val alloc = BudgetCategoryAllocation("a1", "b-1", "cat-food", 40000L)
        val created = repository.createBudget(sampleBudget(allocations = listOf(alloc)), "profile-1")
        assertTrue(created.isSuccess)

        val updated = repository.updateBudget(
            created.getOrThrow().copy(name = "Updated Nov Budget", allocatedAmountMinorUnits = 120000L),
            "profile-1"
        )
        assertTrue(updated.isSuccess)
        assertEquals("Updated Nov Budget", updated.getOrThrow().name)
        assertEquals(120000L, updated.getOrThrow().allocatedAmountMinorUnits)
    }

    @Test
    fun createBudget_rejectsIncomeCategoryAndDuplicateAllocations() = runTest {
        val incomeAlloc = BudgetCategoryAllocation("a1", "b-inc", "cat-salary", 10000L)
        val incomeRes = repository.createBudget(sampleBudget(id = "b-inc", allocations = listOf(incomeAlloc)), "profile-1")
        assertTrue(incomeRes.isFailure)

        val dup1 = BudgetCategoryAllocation("a1", "b-dup", "cat-food", 10000L)
        val dup2 = BudgetCategoryAllocation("a2", "b-dup", "cat-food", 20000L)
        val dupRes = repository.createBudget(sampleBudget(id = "b-dup", allocations = listOf(dup1, dup2)), "profile-1")
        assertTrue(dupRes.isFailure)
    }

    @Test
    fun householdAuthorization_preventsUnauthorizedAccessAndFiltersObservation() = runTest {
        val hh1Budget = sampleBudget(
            id = "b-hh1",
            ownershipType = OwnershipType.HOUSEHOLD,
            profileId = null,
            householdId = "hh-1"
        )
        assertTrue(repository.createBudget(hh1Budget, "profile-1").isSuccess)

        // profile-2 is not a member of hh-1
        assertTrue(repository.getBudgetById("b-hh1", "profile-2").isFailure)
        assertTrue(repository.archiveBudget("b-hh1", "profile-2").isFailure)
        assertTrue(
            repository.createBudget(
                sampleBudget(id = "b-unauth", ownershipType = OwnershipType.HOUSEHOLD, profileId = null, householdId = "hh-1"),
                "profile-2"
            ).isFailure
        )

        val accessibleForP1 = repository.observeAccessibleBudgets("profile-1").first()
        val accessibleForP2 = repository.observeAccessibleBudgets("profile-2").first()
        assertEquals(1, accessibleForP1.size)
        assertEquals(0, accessibleForP2.size)
    }

    @Test
    fun archiveAndUnarchiveBudget_togglesArchivedStatus() = runTest {
        repository.createBudget(sampleBudget(id = "b-arch"), "profile-1").getOrThrow()
        assertTrue(repository.archiveBudget("b-arch", "profile-1").isSuccess)
        assertTrue(repository.getBudgetById("b-arch", "profile-1").getOrThrow().isArchived)

        assertTrue(repository.unarchiveBudget("b-arch", "profile-1").isSuccess)
        assertFalse(repository.getBudgetById("b-arch", "profile-1").getOrThrow().isArchived)
    }
}
