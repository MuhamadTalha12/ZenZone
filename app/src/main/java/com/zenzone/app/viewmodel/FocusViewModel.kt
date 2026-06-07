package com.zenzone.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession
import com.zenzone.app.model.UserProfile
import com.zenzone.app.model.ZenBadge
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.utils.ChainCalculator
import com.zenzone.app.utils.Constants
import com.zenzone.app.utils.DateUtils
import com.zenzone.app.utils.FocusTimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class FocusEvent {
    data class SessionComplete(
        val minutesFocused: Int,
        val oldChain: Int,
        val newChain: Int,
        val xpGained: Int,
        val newlyUnlockedBadges: List<ZenBadge>,
        val completedChallenges: List<String> = emptyList()
    ) : FocusEvent()
    data class Error(val message: String) : FocusEvent()
}

data class SmartRecommendation(
    val recommendedHour: Int,
    val recommendedDuration: Int,
    val displayMessage: String
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val focusRepo = FocusRepository(application)
    private val userRepo = UserRepository(application)
    
    private val _goals = MutableLiveData<List<FocusGoal>>()
    val goals: LiveData<List<FocusGoal>> = _goals
    
    private val _selectedGoal = MutableLiveData<FocusGoal?>()
    val selectedGoal: LiveData<FocusGoal?> = _selectedGoal

    private val _remainingTimeMs = MediatorLiveData<Long>().apply {
        value = 0L
        addSource(FocusTimerService.remainingTimeMs) { time ->
            if (FocusTimerService.isRunning.value == true) {
                value = time
            }
        }
    }
    val remainingTimeMs: LiveData<Long> = _remainingTimeMs
    val totalDurationMs: LiveData<Long> = FocusTimerService.totalDurationMs

    val isRunning: LiveData<Boolean> = FocusTimerService.isRunning
    val isPaused: LiveData<Boolean> = FocusTimerService.isPaused
    val isDndActive: LiveData<Boolean> = FocusTimerService.isDndActive
    val completedPendingLog: LiveData<Pair<FocusGoal, Int>?> = FocusTimerService.isCompletedPendingLog

    private val _focusEvents = MutableLiveData<FocusEvent?>()
    val focusEvents: LiveData<FocusEvent?> = _focusEvents
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _smartRecommendation = MutableLiveData<SmartRecommendation?>()
    val smartRecommendation: LiveData<SmartRecommendation?> = _smartRecommendation

    init {
        _remainingTimeMs.addSource(FocusTimerService.currentGoal) { goal ->
            if (goal != null && FocusTimerService.isRunning.value == true) {
                _selectedGoal.value = goal
            }
        }
    }
    
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profile = userRepo.loadProfile()
                withContext(Dispatchers.Main) {
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadGoals() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val list = focusRepo.loadGoals()
                withContext(Dispatchers.Main) {
                    _goals.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to load goals: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }

    fun selectGoal(goal: FocusGoal?) {
        _selectedGoal.value = goal
        if (goal != null) {
            if (FocusTimerService.isRunning.value != true) {
                _remainingTimeMs.value = goal.targetMinutes * 60 * 1000L
            }
        } else {
            if (FocusTimerService.isRunning.value != true) {
                _remainingTimeMs.value = 0L
            }
        }
    }

    fun startTimer(goal: FocusGoal, useDnd: Boolean, soundscape: String, durationMinutes: Int = -1, distractionSensitivity: String = "GENTLE") {
        if (FocusTimerService.isRunning.value == true) return
        
        val intent = Intent(getApplication(), FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_GOAL, goal)
            putExtra(FocusTimerService.EXTRA_USE_DND, useDnd)
            putExtra(FocusTimerService.EXTRA_SOUNDSCAPE, soundscape)
            putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(FocusTimerService.EXTRA_DISTRACTION_SENSITIVITY, distractionSensitivity)
        }
        getApplication<Application>().startService(intent)
    }

    fun changeSoundscape(soundscape: String) {
        val intent = Intent(getApplication(), FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_CHANGE_SOUNDSCAPE
            putExtra(FocusTimerService.EXTRA_SOUNDSCAPE, soundscape)
        }
        getApplication<Application>().startService(intent)
    }

    fun stopTimer() {
        val intent = Intent(getApplication(), FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    fun pauseTimer() {
        val intent = Intent(getApplication(), FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun resumeTimer() {
        val intent = Intent(getApplication(), FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
    }
    
    fun clearEvent() {
        _focusEvents.value = null
    }

    fun logSession(goal: FocusGoal, minutesFocused: Int, notes: String? = null) {
        viewModelScope.launch {
            try {
                val today = DateUtils.getTodayString()
                val oldChain = goal.currentChain
                val updatedGoal = ChainCalculator.applyChainResult(goal, minutesFocused, today)
                
                // Save goal
                val allGoals = focusRepo.loadGoals().toMutableList()
                val idx = allGoals.indexOfFirst { it.id == goal.id }
                if (idx != -1) {
                    allGoals[idx] = updatedGoal
                } else {
                    allGoals.add(updatedGoal)
                }
                focusRepo.saveGoals(allGoals)

                // Save Session
                val session = FocusSession(
                    id = UUID.randomUUID().toString(),
                    goalId = goal.id,
                    goalName = goal.name,
                    durationMinutes = minutesFocused,
                    completedAt = DateUtils.getIsoTimestamp(),
                    wasChainSaved = updatedGoal.currentChain > oldChain,
                    sessionNotes = notes
                )
                focusRepo.saveSession(session)

                // Update challenge progress
                val challengeRepo = com.zenzone.app.repository.ChallengeRepository(getApplication())
                val completedChallengesList = mutableListOf<String>()
                challengeRepo.updateChallengeProgress("FOCUS_DURATION", minutesFocused)?.let {
                    completedChallengesList.add(it.title)
                }
                challengeRepo.updateChallengeProgress("SESSION_COUNT", 1)?.let {
                    completedChallengesList.add(it.title)
                }
                if (updatedGoal.currentChain > 0) {
                    challengeRepo.updateChallengeProgress("STREAK_MAINTAIN", 1)?.let {
                        completedChallengesList.add(it.title)
                    }
                }
                if (minutesFocused >= goal.targetMinutes) {
                    challengeRepo.updateChallengeProgress("GOAL_COMPLETE", 1)?.let {
                        completedChallengesList.add(it.title)
                    }
                }

                // Update profile
                val profile = userRepo.loadProfile()
                val xpGain = ChainCalculator.calculateXPGain(minutesFocused, updatedGoal.currentChain)
                val newLifetimeXp = profile.zenXP + xpGain
                val (newLevel, _) = ChainCalculator.calculateZenLevel(newLifetimeXp)
                
                val newTotalMs = profile.totalFocusedMinutes + minutesFocused
                val newTotalSess = profile.totalSessions + 1
                val newLongestEver = maxOf(profile.longestEverChain, updatedGoal.currentChain)
                val newProfileChain = allGoals.map { it.currentChain }.maxOrNull() ?: 0

                // Count late night sessions
                val allSessions = focusRepo.loadSessions()
                val lateSessionsCount = allSessions.count { s ->
                    val hour = com.zenzone.app.utils.DateUtils.getHourFromTimestamp(s.completedAt)
                    hour >= 23 || hour < 4
                }

                val (earnedBadges, newBadgesThisSession) = com.zenzone.app.utils.BadgeManager.checkAndUnlockBadges(
                    profile.badges,
                    updatedGoal.currentChain,
                    newTotalMs,
                    newTotalSess,
                    lateSessionsCount
                )

                val updatedProfile = profile.copy(
                    totalFocusedMinutes = newTotalMs,
                    zenLevel = newLevel,
                    zenXP = newLifetimeXp,
                    badges = earnedBadges,
                    totalSessions = newTotalSess,
                    longestEverChain = newLongestEver,
                    currentChain = newProfileChain
                )
                userRepo.saveProfile(updatedProfile)
                com.zenzone.app.ui.widget.FocusChainWidgetProvider.triggerWidgetUpdate(getApplication())
                loadSmartRecommendation()
 
                withContext(Dispatchers.Main) {
                    _userProfile.value = updatedProfile
                    _goals.value = allGoals
                    _selectedGoal.value = updatedGoal
                    _focusEvents.value = FocusEvent.SessionComplete(
                        minutesFocused,
                        oldChain,
                        updatedGoal.currentChain,
                        xpGain,
                        newBadgesThisSession,
                        completedChallengesList
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to log session: ${e.message}"
                }
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun loadSmartRecommendation() {
        viewModelScope.launch {
            try {
                val sessions = focusRepo.loadSessions()
                if (sessions.isEmpty()) {
                    _smartRecommendation.postValue(
                        SmartRecommendation(
                            recommendedHour = 10,
                            recommendedDuration = 25,
                            displayMessage = "You focus best at 10 AM for 25-minute blocks - want to schedule one?"
                        )
                    )
                    return@launch
                }

                // Calculate best time (peak hour of completion)
                val hoursCount = sessions.map { DateUtils.getHourFromTimestamp(it.completedAt) }
                    .filter { it >= 0 }
                    .groupingBy { it }
                    .eachCount()

                val bestHour = if (hoursCount.isNotEmpty()) {
                    hoursCount.maxByOrNull { it.value }?.key ?: 10
                } else {
                    10
                }

                // Calculate best length (average duration rounded to nearest 5 minutes)
                val avgDuration = sessions.map { it.durationMinutes }.average()
                val roundedDuration = if (avgDuration.isNaN()) {
                    25
                } else {
                    val rawRounded = ((avgDuration + 2.5) / 5).toInt() * 5
                    rawRounded.coerceIn(5, 180)
                }

                val amPmHour = when {
                    bestHour == 0 -> "12 AM"
                    bestHour < 12 -> "$bestHour AM"
                    bestHour == 12 -> "12 PM"
                    else -> "${bestHour - 12} PM"
                }

                _smartRecommendation.postValue(
                    SmartRecommendation(
                        recommendedHour = bestHour,
                        recommendedDuration = roundedDuration,
                        displayMessage = "You focus best at $amPmHour for $roundedDuration-minute blocks - want to schedule one?"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
