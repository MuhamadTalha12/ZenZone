package com.zenzone.app.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zenzone.app.model.FocusSession

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions")
    suspend fun getAllSessions(): List<FocusSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<FocusSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM focus_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<FocusSession>

    @Query("UPDATE focus_sessions SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
