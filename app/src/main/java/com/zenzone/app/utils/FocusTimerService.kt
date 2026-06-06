package com.zenzone.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zenzone.app.R
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.ui.main.MainActivity
import kotlinx.coroutines.*

class FocusTimerService : Service() {

    private var countdownTimer: CountDownTimer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var appCheckJob: Job? = null
    private var lastNudgeTime = 0L
    private lateinit var soundscapePlayer: SoundscapePlayer
    private var initialDurationMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        soundscapePlayer = SoundscapePlayer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                @Suppress("DEPRECATION")
                val goal = intent.getSerializableExtra(EXTRA_GOAL) as? FocusGoal
                val useDnd = intent.getBooleanExtra(EXTRA_USE_DND, false)
                val soundscape = intent.getStringExtra(EXTRA_SOUNDSCAPE) ?: SoundscapePlayer.SOUNDSCAPE_NONE
                
                val modeDuration = intent.getIntExtra(EXTRA_DURATION_MINUTES, -1)
                val sensitivity = intent.getStringExtra(EXTRA_DISTRACTION_SENSITIVITY) ?: "GENTLE"
                
                val durationMs = if (modeDuration > 0) {
                    modeDuration * 60 * 1000L
                } else {
                    goal?.targetMinutes?.times(60)?.times(1000L) ?: (25 * 60 * 1000L)
                }
                
                if (goal != null) {
                    _distractionSensitivity.postValue(sensitivity)
                    startTimer(goal, durationMs, useDnd, soundscape)
                }
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_RESUME -> {
                resumeTimer()
            }
            ACTION_STOP -> {
                stopTimer()
            }
            ACTION_CHANGE_SOUNDSCAPE -> {
                val soundscape = intent.getStringExtra(EXTRA_SOUNDSCAPE) ?: SoundscapePlayer.SOUNDSCAPE_NONE
                changeSoundscape(soundscape)
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(goal: FocusGoal, durationMs: Long, useDnd: Boolean, soundscape: String) {
        countdownTimer?.cancel()
        appCheckJob?.cancel()
        
        initialDurationMs = durationMs
        _currentGoal.postValue(goal)
        _remainingTimeMs.postValue(durationMs)
        _isRunning.postValue(true)
        _isPaused.postValue(false)
        _isDndActive.postValue(useDnd)
        _isCompletedPendingLog.postValue(null)
        _currentSoundscape.postValue(soundscape)

        if (useDnd) {
            DndHelper.enableDnd(this)
        }

        soundscapePlayer.playSound(soundscape)

        startForegroundServiceCompat(goal, durationMs)

        createTimer(durationMs)
        startAppChecking()
    }

    private fun createTimer(durationMs: Long) {
        countdownTimer = object : CountDownTimer(durationMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingTimeMs.postValue(millisUntilFinished)
                updateNotification(millisUntilFinished)
            }

            override fun onFinish() {
                handleTimerFinished()
            }
        }.start()
    }

    private fun pauseTimer() {
        if (_isRunning.value == true && _isPaused.value == false) {
            countdownTimer?.cancel()
            appCheckJob?.cancel()
            soundscapePlayer.pause()
            _isPaused.postValue(true)
            _currentGoal.value?.let { goal ->
                updateNotificationPaused(goal)
            }
        }
    }

    private fun resumeTimer() {
        if (_isRunning.value == true && _isPaused.value == true) {
            val remain = _remainingTimeMs.value ?: 0L
            _isPaused.postValue(false)
            _currentGoal.value?.let { goal ->
                updateNotification(remain)
            }
            soundscapePlayer.resume()
            createTimer(remain)
            startAppChecking()
        }
    }

    private fun stopTimer() {
        countdownTimer?.cancel()
        appCheckJob?.cancel()
        soundscapePlayer.stop()
        if (_isDndActive.value == true) {
            DndHelper.disableDnd(this)
        }
        _isRunning.postValue(false)
        _isPaused.postValue(false)
        _currentGoal.postValue(null)
        _remainingTimeMs.postValue(0L)
        _isDndActive.postValue(false)
        _currentSoundscape.postValue(SoundscapePlayer.SOUNDSCAPE_NONE)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleTimerFinished() {
        appCheckJob?.cancel()
        soundscapePlayer.stop()
        val goal = _currentGoal.value
        if (goal != null) {
            val minutes = (initialDurationMs / 60 / 1000L).toInt()
            _isCompletedPendingLog.postValue(Pair(goal, minutes))
            showFinishedNotification(goal)
        }
        if (_isDndActive.value == true) {
            DndHelper.disableDnd(this)
        }
        _isRunning.postValue(false)
        _isPaused.postValue(false)
        _currentGoal.postValue(null)
        _remainingTimeMs.postValue(0L)
        _isDndActive.postValue(false)
        _currentSoundscape.postValue(SoundscapePlayer.SOUNDSCAPE_NONE)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundServiceCompat(goal: FocusGoal, durationMs: Long) {
        val notification = buildTimerNotification(goal, durationMs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildTimerNotification(goal: FocusGoal, remainMs: Long): Notification {
        val totalSecs = remainMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        val timeStr = String.format("%02d:%02d left", mins, secs)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions
        val pauseIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
        val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focusing: ${goal.name}")
            .setContentText(timeStr)
            .setSmallIcon(R.drawable.ic_focus)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_focus, "Pause", pausePending)
            .addAction(R.drawable.ic_stats, "Stop", stopPending)
            .build()
    }

    private fun updateNotification(remainMs: Long) {
        val goal = _currentGoal.value ?: return
        val notification = buildTimerNotification(goal, remainMs)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationPaused(goal: FocusGoal) {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val resumeIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_RESUME }
        val resumePending = PendingIntent.getService(this, 3, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Paused: ${goal.name}")
            .setContentText("Paused")
            .setSmallIcon(R.drawable.ic_focus)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_focus, "Resume", resumePending)
            .addAction(R.drawable.ic_stats, "Cancel", stopPending)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFinishedNotification(goal: FocusGoal) {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zen Session Complete! 🎉")
            .setContentText("Great job completing your session for ${goal.name}.")
            .setSmallIcon(R.drawable.ic_focus)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ZenZone Focus Timer"
            val descriptionText = "Displays the current active deep focus session countdown."
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startAppChecking() {
        appCheckJob?.cancel()
        val sensitivity = _distractionSensitivity.value ?: "GENTLE"
        if (sensitivity == "OFF") return
        
        val checkInterval = if (sensitivity == "STRICT") 1500L else 5000L
        
        appCheckJob = serviceScope.launch {
            while (isActive) {
                delay(checkInterval)
                if (_isRunning.value == true && _isPaused.value == false) {
                    if (UsageStatsHelper.hasUsageStatsPermission(this@FocusTimerService)) {
                        val fgApp = UsageStatsHelper.getForegroundApp(this@FocusTimerService)
                        if (fgApp != null && fgApp != packageName) {
                            val isBlocked = isAppBlockedCustomOrHardcoded(fgApp)
                            if (isBlocked) {
                                showNudgeNotification()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun isAppBlockedCustomOrHardcoded(packageName: String): Boolean {
        try {
            val db = com.zenzone.app.repository.AppDatabase.getDatabase(applicationContext)
            val isCustomBlocked = db.blockedAppDao().isAppBlocked(packageName)
            if (isCustomBlocked) return true
            
            val customApp = db.blockedAppDao().getAllBlockedApps().firstOrNull { it.packageName == packageName }
            if (customApp != null) {
                return customApp.isBlocked
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return UsageStatsHelper.isDistractingApp(packageName)
    }

    private fun showNudgeNotification() {
        val now = System.currentTimeMillis()
        if (now - lastNudgeTime < 10000L) return
        lastNudgeTime = now

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 100, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gentle Nudge 🧘‍♂️")
            .setContentText("Stay focused! Return to ZenZone to complete your session.")
            .setSmallIcon(R.drawable.ic_focus)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun changeSoundscape(soundscape: String) {
        _currentSoundscape.postValue(soundscape)
        if (_isRunning.value == true && _isPaused.value == false) {
            soundscapePlayer.playSound(soundscape)
        }
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        appCheckJob?.cancel()
        serviceScope.cancel()
        soundscapePlayer.stop()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "zenzone_timer_channel"
        private const val NOTIFICATION_ID = 1001

        private val _remainingTimeMs = MutableLiveData<Long>(0L)
        val remainingTimeMs: LiveData<Long> = _remainingTimeMs

        private val _isRunning = MutableLiveData<Boolean>(false)
        val isRunning: LiveData<Boolean> = _isRunning

        private val _isPaused = MutableLiveData<Boolean>(false)
        val isPaused: LiveData<Boolean> = _isPaused

        private val _isDndActive = MutableLiveData<Boolean>(false)
        val isDndActive: LiveData<Boolean> = _isDndActive

        private val _currentGoal = MutableLiveData<FocusGoal?>(null)
        val currentGoal: LiveData<FocusGoal?> = _currentGoal

        private val _isCompletedPendingLog = MutableLiveData<Pair<FocusGoal, Int>?>(null)
        val isCompletedPendingLog: LiveData<Pair<FocusGoal, Int>?> = _isCompletedPendingLog

        private val _currentSoundscape = MutableLiveData<String>(SoundscapePlayer.SOUNDSCAPE_NONE)
        val currentSoundscape: LiveData<String> = _currentSoundscape

        private val _distractionSensitivity = MutableLiveData<String>("GENTLE")
        val distractionSensitivity: LiveData<String> = _distractionSensitivity

        // Actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CHANGE_SOUNDSCAPE = "ACTION_CHANGE_SOUNDSCAPE"

        // Intent Keys
        const val EXTRA_GOAL = "EXTRA_GOAL"
        const val EXTRA_USE_DND = "EXTRA_USE_DND"
        const val EXTRA_REMAINING_MS = "EXTRA_REMAINING_MS"
        const val EXTRA_SOUNDSCAPE = "EXTRA_SOUNDSCAPE"
        const val EXTRA_DURATION_MINUTES = "EXTRA_DURATION_MINUTES"
        const val EXTRA_DISTRACTION_SENSITIVITY = "EXTRA_DISTRACTION_SENSITIVITY"

        fun clearPendingLog() {
            _isCompletedPendingLog.postValue(null)
        }
    }
}
