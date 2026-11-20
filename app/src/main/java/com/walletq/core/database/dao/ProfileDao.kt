package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.walletq.core.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY isDefault DESC, name ASC")
    fun getAllProfilesFlow(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    fun getDefaultProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getCount(): Int
}
