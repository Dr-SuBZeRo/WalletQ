package com.walletq.core.database

import androidx.room.TypeConverter
import com.walletq.core.model.AccountType
import com.walletq.core.model.BudgetPeriod
import com.walletq.core.model.HouseholdRole
import com.walletq.core.model.LedgerEntryType
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import java.math.BigDecimal

class Converters {

    @TypeConverter
    fun fromOwnershipType(type: OwnershipType): String = type.name

    @TypeConverter
    fun toOwnershipType(value: String): OwnershipType = try {
        OwnershipType.valueOf(value)
    } catch (e: Exception) {
        OwnershipType.PERSONAL
    }

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = try {
        AccountType.valueOf(value)
    } catch (e: Exception) {
        AccountType.OTHER
    }

    @TypeConverter
    fun fromHouseholdRole(role: HouseholdRole): String = role.name

    @TypeConverter
    fun toHouseholdRole(value: String): HouseholdRole = try {
        HouseholdRole.valueOf(value)
    } catch (e: Exception) {
        HouseholdRole.MEMBER
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus = try {
        TransactionStatus.valueOf(value)
    } catch (e: Exception) {
        TransactionStatus.POSTED
    }

    @TypeConverter
    fun fromLedgerEntryType(type: LedgerEntryType): String = type.name

    @TypeConverter
    fun toLedgerEntryType(value: String): LedgerEntryType = try {
        LedgerEntryType.valueOf(value)
    } catch (e: Exception) {
        LedgerEntryType.DEBIT
    }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun fromBudgetPeriod(period: BudgetPeriod): String = period.name

    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod = try {
        BudgetPeriod.valueOf(value)
    } catch (e: Exception) {
        BudgetPeriod.MONTHLY
    }
}
