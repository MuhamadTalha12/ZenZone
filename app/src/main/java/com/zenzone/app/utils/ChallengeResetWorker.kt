package com.zenzone.app.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenzone.app.repository.ChallengeRepository
import kotlinx.coroutines.CancellationException

class ChallengeResetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val challengeRepo = ChallengeRepository(context)
        val today = DateUtils.getTodayString()

        try {
            // 1. Clear yesterday's Room challenge rows
            challengeRepo.deleteOldChallenges(today)

            // 2. Fetch new challenges from Firestore for today
            val success = challengeRepo.fetchAndCacheChallenges(today)
            if (!success) {
                // If fetching failed or today's challenges don't exist yet, seed defaults
                challengeRepo.generateAndUploadDefaultChallenges(today)
            }

            return Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
