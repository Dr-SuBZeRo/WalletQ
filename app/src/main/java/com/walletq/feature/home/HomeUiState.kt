package com.walletq.feature.home

import com.walletq.core.model.Account
import com.walletq.core.model.BudgetSpendingSummary
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.Transaction
import com.walletq.feature.activity.OwnershipFilter
import java.time.YearMonth

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profileName: String = "Personal",
    val selectedMonth: YearMonth = YearMonth.now(),
    val ownershipFilter: OwnershipFilter = OwnershipFilter.ALL,
    val isAmountsHidden: Boolean = false,
    val reportingCurrency: Currency = Currency.DEFAULT_REPORTING_CURRENCY,
    val netWorthMinorUnits: Long = 0L,
    val otherCurrencyAccountCount: Int = 0,
    val monthlyIncomeMinorUnits: Long = 0L,
    val monthlyExpenseMinorUnits: Long = 0L,
    val netCashFlowMinorUnits: Long = 0L,
    val accounts: List<Account> = emptyList(),
    val accountsById: Map<String, Account> = emptyMap(),
    val householdsById: Map<String, Household> = emptyMap(),
    val categoriesById: Map<String, Category> = emptyMap(),
    val activeBudgets: List<BudgetSpendingSummary> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList()
)
