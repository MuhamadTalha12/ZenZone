package com.zenzone.app.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession
import com.zenzone.app.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object JsonStorageHelper {
    private const val FILE_GOALS = "focus_goals.json"
    private const val FILE_SESSIONS = "focus_sessions.json"
    private const val FILE_PROFILE = "user_profile.json"
    private val gson = Gson()

    suspend fun saveGoals(context: Context, goals: List<FocusGoal>) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_GOALS)
        FileWriter(file).use { writer ->
            gson.toJson(goals, writer)
        }
    }

    suspend fun loadGoals(context: Context): List<FocusGoal> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_GOALS)
        if (!file.exists()) return@withContext emptyList()
        FileReader(file).use { reader ->
            val type = object : TypeToken<List<FocusGoal>>() {}.type
            gson.fromJson(reader, type) ?: emptyList()
        }
    }

    suspend fun saveSession(context: Context, session: FocusSession) = withContext(Dispatchers.IO) {
        val sessions = loadSessions(context).toMutableList()
        sessions.add(session)
        val file = File(context.filesDir, FILE_SESSIONS)
        FileWriter(file).use { writer ->
            gson.toJson(sessions, writer)
        }
    }

    suspend fun loadSessions(context: Context): List<FocusSession> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_SESSIONS)
        if (!file.exists()) return@withContext emptyList()
        FileReader(file).use { reader ->
            val type = object : TypeToken<List<FocusSession>>() {}.type
            gson.fromJson(reader, type) ?: emptyList()
        }
    }

    suspend fun saveProfile(context: Context, profile: UserProfile) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_PROFILE)
        FileWriter(file).use { writer ->
            gson.toJson(profile, writer)
        }
    }

    suspend fun loadProfile(context: Context): UserProfile = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_PROFILE)
        if (!file.exists()) {
            val defaultProfile = UserProfile(
                userName = "",
                totalFocusedMinutes = 0,
                zenLevel = 1,
                zenXP = 0,
                badges = emptyList(),
                totalSessions = 0,
                longestEverChain = 0
            )
            saveProfile(context, defaultProfile)
            return@withContext defaultProfile
        }
        FileReader(file).use { reader ->
            val type = object : TypeToken<UserProfile>() {}.type
            gson.fromJson(reader, type)
        }
    }
}
