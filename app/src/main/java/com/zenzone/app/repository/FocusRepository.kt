package com.zenzone.app.repository

import android.content.Context
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession
import com.zenzone.app.utils.JsonStorageHelper

class FocusRepository(private val context: Context) {
    suspend fun loadGoals(): List<FocusGoal> = JsonStorageHelper.loadGoals(context)
    suspend fun saveGoals(goals: List<FocusGoal>) = JsonStorageHelper.saveGoals(context, goals)
    suspend fun loadSessions(): List<FocusSession> = JsonStorageHelper.loadSessions(context)
    suspend fun saveSession(session: FocusSession) = JsonStorageHelper.saveSession(context, session)
}
