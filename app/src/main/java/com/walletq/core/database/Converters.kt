package com.walletq.core.database

import androidx.room.TypeConverter
import com.walletq.core.model.AccountType
import com.walletq.core.model.OwnershipType

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
}
