package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(household: HouseholdEntity)

    @Update
    suspend fun update(household: HouseholdEntity)

    @Delete
    suspend fun delete(household: HouseholdEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: HouseholdMemberEntity)

    @Query("DELETE FROM household_members WHERE householdId = :householdId AND profileId = :profileId")
    suspend fun deleteMember(householdId: String, profileId: String)

    @Query("SELECT * FROM households WHERE id = :id")
    suspend fun getById(id: String): HouseholdEntity?

    @Query("SELECT * FROM households ORDER BY name ASC")
    fun getAllHouseholdsFlow(): Flow<List<HouseholdEntity>>

    @Query("SELECT * FROM household_members WHERE householdId = :householdId ORDER BY joinedAtEpochMs ASC")
    fun getMembersForHouseholdFlow(householdId: String): Flow<List<HouseholdMemberEntity>>

    @Query("SELECT * FROM household_members WHERE householdId = :householdId ORDER BY joinedAtEpochMs ASC")
    suspend fun getMembersForHousehold(householdId: String): List<HouseholdMemberEntity>

    @Query("""
        SELECT h.* FROM households h
        INNER JOIN household_members hm ON h.id = hm.householdId
        WHERE hm.profileId = :profileId
        ORDER BY h.name ASC
    """)
    fun getHouseholdsForProfileFlow(profileId: String): Flow<List<HouseholdEntity>>

    @Query("SELECT COUNT(*) FROM household_members WHERE householdId = :householdId")
    suspend fun getMemberCount(householdId: String): Int

    @Query("SELECT COUNT(*) FROM households")
    suspend fun getHouseholdCount(): Int
}
