package com.zenzone.app.repository

import android.content.Context
import androidx.work.*
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession
import com.zenzone.app.utils.JsonStorageHelper
import com.zenzone.app.utils.SyncWorker

class FocusRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val goalDao = db.focusGoalDao()
    private val sessionDao = db.focusSessionDao()

    suspend fun loadGoals(): List<FocusGoal> {
        var goals = goalDao.getAllGoals()
        if (goals.isEmpty()) {
            val jsonGoals = JsonStorageHelper.loadGoals(context)
            if (jsonGoals.isNotEmpty()) {
                goalDao.insertGoals(jsonGoals)
                goals = goalDao.getAllGoals()
            }
        }
        return goals
    }
    
    suspend fun saveGoals(goals: List<FocusGoal>) {
        val goalsToSave = goals.map { it.copy(isSynced = false) }
        goalDao.deleteAllGoals()
        goalDao.insertGoals(goalsToSave)
        JsonStorageHelper.saveGoals(context, goalsToSave)
        enqueueSync()
    }
    
    suspend fun loadSessions(): List<FocusSession> {
        var sessions = sessionDao.getAllSessions()
        if (sessions.isEmpty()) {
            val jsonSessions = JsonStorageHelper.loadSessions(context)
            if (jsonSessions.isNotEmpty()) {
                sessionDao.insertSessions(jsonSessions)
                sessions = sessionDao.getAllSessions()
            }
        }
        return sessions
    }
    
    suspend fun saveSession(session: FocusSession) {
        val sessionToSave = session.copy(isSynced = false)
        sessionDao.insertSession(sessionToSave)
        JsonStorageHelper.saveSession(context, sessionToSave)
        enqueueSync()
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            "zenzone_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
