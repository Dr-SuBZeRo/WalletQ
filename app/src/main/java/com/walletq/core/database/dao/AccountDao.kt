package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.walletq.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND isArchived = 0 ORDER BY name ASC")
    fun getAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE householdId = :householdId AND isArchived = 0 ORDER BY name ASC")
    fun getAccountsByHouseholdFlow(householdId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND ownershipType = 'PERSONAL' AND isArchived = 0 ORDER BY name ASC")
    fun getPersonalAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllActiveAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, name ASC")
    fun observeAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeAccountByIdFlow(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND ownershipType = 'PERSONAL' AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByNameInPersonalScope(profileId: String, name: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE householdId = :householdId AND ownershipType = 'HOUSEHOLD' AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByNameInHouseholdScope(householdId: String, name: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getCount(): Int

    @Query("UPDATE accounts SET currentBalanceMinorUnits = :balance WHERE id = :id")
    suspend fun updateCurrentBalance(id: String, balance: Long)

    @Query("UPDATE accounts SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: String, isArchived: Boolean)
}
