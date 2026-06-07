package com.zenzone.app.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zenzone.app.model.BlockedAppEntity

@Dao
interface BlockedAppDao {

    @Query("SELECT isBlocked FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun isAppBlocked(packageName: String): Boolean?

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAllBlockedApps(): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApps(apps: List<BlockedAppEntity>)
}
