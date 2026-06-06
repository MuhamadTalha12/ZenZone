package com.zenzone.app.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenzone.app.repository.AppDatabase

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!FirebaseSyncManager.isUserSignedIn()) {
            return Result.success()
        }

        val db = AppDatabase.getDatabase(context)
        val goalDao = db.focusGoalDao()
        val sessionDao = db.focusSessionDao()

        try {
            // 1. Sync unsynced goals
            val unsyncedGoals = goalDao.getUnsyncedGoals()
            for (goal in unsyncedGoals) {
                FirebaseSyncManager.saveGoalToFirestore(goal)
                goalDao.markSynced(goal.id)
            }

            // 2. Sync unsynced sessions
            val unsyncedSessions = sessionDao.getUnsyncedSessions()
            for (session in unsyncedSessions) {
                FirebaseSyncManager.saveSessionToFirestore(session)
                sessionDao.markSynced(session.id)
            }

            // 3. Keep profile synced
            val localProfile = JsonStorageHelper.loadProfile(context)
            if (localProfile.userName.isNotBlank()) {
                FirebaseSyncManager.saveProfileToFirestore(localProfile)
            }

            return Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
