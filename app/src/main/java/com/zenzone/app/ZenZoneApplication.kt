package com.zenzone.app

import android.app.Application
import androidx.work.*
import com.zenzone.app.utils.ChallengeResetWorker
import com.zenzone.app.utils.FirebaseSyncManager
import com.zenzone.app.utils.SyncWorker
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ZenZoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleChallengeResetWorker()
        if (FirebaseSyncManager.isUserSignedIn()) {
            SyncWorker.enqueuePeriodicSync(this)
        }
    }

    private fun scheduleChallengeResetWorker() {
        val delay = calculateDelayToMidnightUtc()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val resetRequest = PeriodicWorkRequestBuilder<ChallengeResetWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "challenge_reset_work",
            ExistingPeriodicWorkPolicy.KEEP,
            resetRequest
        )
    }

    private fun calculateDelayToMidnightUtc(): Long {
        val utc = TimeZone.getTimeZone("UTC")
        val now = Calendar.getInstance(utc)
        val nextMidnight = Calendar.getInstance(utc).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return nextMidnight.timeInMillis - now.timeInMillis
    }
}
