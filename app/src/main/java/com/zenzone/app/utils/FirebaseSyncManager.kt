package com.zenzone.app.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession
import com.zenzone.app.model.UserProfile
import com.zenzone.app.repository.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseSyncManager {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    var lastSyncError: String? = null
        private set

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    suspend fun clearAllLocalData(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.focusGoalDao().deleteAllGoals()
            db.focusSessionDao().deleteAllSessions()
            db.challengeDao().deleteAllChallenges()

            val files = listOf("focus_goals.json", "focus_sessions.json", "user_profile.json")
            for (fileName in files) {
                val file = java.io.File(context.filesDir, fileName)
                if (file.exists()) {
                    file.delete()
                }
            }

            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(Constants.PREF_USER_NAME).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncLocalToFirestore(context: Context) = withContext(Dispatchers.IO) {
        val uid = getCurrentUserUid() ?: return@withContext
        try {
            // 1. Sync Profile
            val localProfile = JsonStorageHelper.loadProfile(context)
            firestore.collection("users").document(uid)
                .set(localProfile, SetOptions.merge())
                .await()

            // 2. Sync Goals
            val localGoals = JsonStorageHelper.loadGoals(context)
            val goalsCollection = firestore.collection("users").document(uid).collection("goals")
            for (goal in localGoals) {
                goalsCollection.document(goal.id).set(goal, SetOptions.merge()).await()
            }

            // 3. Sync Sessions
            val localSessions = JsonStorageHelper.loadSessions(context)
            val sessionsCollection = firestore.collection("users").document(uid).collection("sessions")
            for (session in localSessions) {
                sessionsCollection.document(session.id).set(session, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncFirestoreToLocal(context: Context): Boolean = withContext(Dispatchers.IO) {
        val uid = getCurrentUserUid() ?: return@withContext false
        try {
            // 1. Sync Profile
            val profileDoc = firestore.collection("users").document(uid).get().await()
            if (profileDoc.exists()) {
                val remoteProfile = profileDoc.toObject(UserProfile::class.java)
                if (remoteProfile != null) {
                    JsonStorageHelper.saveProfile(context, remoteProfile)
                    
                    // Also save name in shared preferences to match onboarding flow
                    val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putString(Constants.PREF_USER_NAME, remoteProfile.userName).apply()
                }
            }

            val db = AppDatabase.getDatabase(context)

            // 2. Sync Goals
            val goalsSnapshot = firestore.collection("users").document(uid).collection("goals").get().await()
            val remoteGoals = goalsSnapshot.documents.mapNotNull { it.toObject(FocusGoal::class.java) }
            if (remoteGoals.isNotEmpty()) {
                JsonStorageHelper.saveGoals(context, remoteGoals)
                db.focusGoalDao().deleteAllGoals()
                db.focusGoalDao().insertGoals(remoteGoals)
            }

            // 3. Sync Sessions
            val sessionsSnapshot = firestore.collection("users").document(uid).collection("sessions").get().await()
            val remoteSessions = sessionsSnapshot.documents.mapNotNull { it.toObject(FocusSession::class.java) }
            if (remoteSessions.isNotEmpty()) {
                // Save loaded sessions locally
                val localSessions = JsonStorageHelper.loadSessions(context).toMutableList()
                for (remote in remoteSessions) {
                    if (localSessions.none { it.id == remote.id }) {
                        localSessions.add(remote)
                    }
                }
                // Save back to JSON
                val file = java.io.File(context.filesDir, "focus_sessions.json")
                val gson = com.google.gson.Gson()
                java.io.FileWriter(file).use { writer ->
                    gson.toJson(localSessions, writer)
                }

                // Save loaded/merged sessions to Room
                db.focusSessionDao().deleteAllSessions()
                db.focusSessionDao().insertSessions(localSessions)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            lastSyncError = e.message ?: e.toString()
            false
        }
    }

    suspend fun saveProfileToFirestore(profile: UserProfile) {
        val uid = getCurrentUserUid() ?: return
        try {
            firestore.collection("users").document(uid)
                .set(profile, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveGoalToFirestore(goal: FocusGoal) {
        val uid = getCurrentUserUid() ?: return
        try {
            firestore.collection("users").document(uid).collection("goals")
                .document(goal.id)
                .set(goal, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveSessionToFirestore(session: FocusSession) {
        val uid = getCurrentUserUid() ?: return
        try {
            firestore.collection("users").document(uid).collection("sessions")
                .document(session.id)
                .set(session, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
