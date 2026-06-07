package com.zenzone.app.repository

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zenzone.app.model.ChallengeEntity

@Dao
interface ChallengeDao {

    @Query("SELECT * FROM daily_challenges WHERE date = :date")
    fun getTodaysChallenges(date: String): LiveData<List<ChallengeEntity>>

    @Query("SELECT * FROM daily_challenges WHERE date = :date")
    suspend fun getChallengesForDateSync(date: String): List<ChallengeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<ChallengeEntity>)

    @Query("DELETE FROM daily_challenges WHERE date != :date")
    suspend fun deleteOldChallenges(date: String)

    @Query("UPDATE daily_challenges SET currentProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)

    @Query("UPDATE daily_challenges SET isCompleted = 1 WHERE id = :id")
    suspend fun markComplete(id: String)

    @Query("DELETE FROM daily_challenges")
    suspend fun deleteAllChallenges()

    @Query("SELECT * FROM daily_challenges")
    suspend fun getAllChallenges(): List<ChallengeEntity>
}
