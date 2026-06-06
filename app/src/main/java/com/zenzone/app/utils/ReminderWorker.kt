package com.zenzone.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenzone.app.R
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.ui.main.MainActivity

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val repo = FocusRepository(applicationContext)
            val sessions = repo.loadSessions()
            val todayStr = DateUtils.getTodayString()

            val hasFocusedToday = sessions.any { it.completedAt.take(10) == todayStr }

            if (!hasFocusedToday) {
                val randomMessage = Constants.MOTIVATIONAL_REMINDERS.random()
                showReminderNotification(randomMessage)
            }
            return Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun showReminderNotification(message: String) {
        val channelId = "zenzone_reminder_channel"
        val notificationId = 2002

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ZenZone Daily Reminder"
            val desc = "Encourages daily focus sessions to protect your streak."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "focus")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Protect your Focus Chain! 🔥")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_focus)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
