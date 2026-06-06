package com.zenzone.app.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zenzone.app.model.FocusGoal

@Dao
interface FocusGoalDao {

    @Query("SELECT * FROM focus_goals")
    suspend fun getAllGoals(): List<FocusGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<FocusGoal>)

    @Query("DELETE FROM focus_goals")
    suspend fun deleteAllGoals()

    @Query("SELECT * FROM focus_goals WHERE isSynced = 0")
    suspend fun getUnsyncedGoals(): List<FocusGoal>

    @Query("UPDATE focus_goals SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
