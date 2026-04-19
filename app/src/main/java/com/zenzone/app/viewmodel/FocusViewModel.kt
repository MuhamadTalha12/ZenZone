package com.zenzone.app.viewmodel

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession
import com.zenzone.app.model.ZenBadge
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.utils.ChainCalculator
import com.zenzone.app.utils.Constants
import com.zenzone.app.utils.DateUtils
import com.zenzone.app.utils.DndHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class FocusEvent {
    data class SessionComplete(val minutesFocused: Int, val oldChain: Int, val newChain: Int, val xpGained: Int, val newlyUnlockedBadges: List<ZenBadge>) : FocusEvent()
    data class Error(val message: String) : FocusEvent()
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val focusRepo = FocusRepository(application)
    private val userRepo = UserRepository(application)
    
    private val _goals = MutableLiveData<List<FocusGoal>>()
    val goals: LiveData<List<FocusGoal>> = _goals
    
    private val _selectedGoal = MutableLiveData<FocusGoal?>()
    val selectedGoal: LiveData<FocusGoal?> = _selectedGoal

    private val _remainingTimeMs = MutableLiveData<Long>()
    val remainingTimeMs: LiveData<Long> = _remainingTimeMs

    private val _isRunning = MutableLiveData<Boolean>(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _isDndActive = MutableLiveData<Boolean>(false)
    val isDndActive: LiveData<Boolean> = _isDndActive

    private val _focusEvents = MutableLiveData<FocusEvent?>()
    val focusEvents: LiveData<FocusEvent?> = _focusEvents
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private var timer: CountDownTimer? = null
    
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

    fun selectGoal(goal: FocusGoal) {
        _selectedGoal.value = goal
        if (_isRunning.value != true) {
            _remainingTimeMs.value = goal.targetMinutes * 60 * 1000L
        }
    }

    fun startTimer(goal: FocusGoal, useDnd: Boolean) {
        if (_isRunning.value == true) return
        
        if (useDnd) {
            DndHelper.enableDnd(getApplication())
            _isDndActive.value = true
        }

        val totalMs = _remainingTimeMs.value ?: (goal.targetMinutes * 60 * 1000L)
        
        timer = object : CountDownTimer(totalMs, Constants.TIMER_TICK_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingTimeMs.value = millisUntilFinished
            }

            override fun onFinish() {
                _remainingTimeMs.value = 0
                _isRunning.value = false
                if (_isDndActive.value == true) {
                    DndHelper.disableDnd(getApplication())
                    _isDndActive.value = false
                }
                
                _focusEvents.value = FocusEvent.SessionComplete(goal.targetMinutes, goal.currentChain, goal.currentChain, 0, emptyList())
            }
        }.start()
        
        _isRunning.value = true
    }

    fun stopTimer() {
        timer?.cancel()
        _isRunning.value = false
        if (_isDndActive.value == true) {
            DndHelper.disableDnd(getApplication())
            _isDndActive.value = false
        }
    }
    
    fun clearEvent() {
        _focusEvents.value = null
    }

    fun logSession(goal: FocusGoal, minutesFocused: Int) {
        viewModelScope.launch {
            try {
                val today = DateUtils.getTodayString()
                val oldChain = goal.currentChain
                val updatedGoal = ChainCalculator.applyChainResult(goal, minutesFocused, today)
                
                // Save goal
                val allGoals = focusRepo.loadGoals().toMutableList()
                val idx = allGoals.indexOfFirst { it.id == goal.id }
                if (idx != -1) allGoals[idx] = updatedGoal
                focusRepo.saveGoals(allGoals)

                // Save Session
                val session = FocusSession(
                    id = UUID.randomUUID().toString(),
                    goalId = goal.id,
                    goalName = goal.name,
                    durationMinutes = minutesFocused,
                    completedAt = DateUtils.getIsoTimestamp(),
                    wasChainSaved = updatedGoal.currentChain > oldChain
                )
                focusRepo.saveSession(session)

                // Update profile
                val profile = userRepo.loadProfile()
                val xpGain = ChainCalculator.calculateXPGain(minutesFocused, updatedGoal.currentChain)
                val newLifetimeXp = profile.zenXP + xpGain
                val (newLevel, _) = ChainCalculator.calculateZenLevel(newLifetimeXp)
                
                val newTotalMs = profile.totalFocusedMinutes + minutesFocused
                val newTotalSess = profile.totalSessions + 1
                val newLongestEver = maxOf(profile.longestEverChain, updatedGoal.currentChain)

                val (earnedBadges, newBadgesThisSession) = com.zenzone.app.utils.BadgeManager.checkAndUnlockBadges(
                    profile.badges,
                    updatedGoal.currentChain,
                    newTotalMs,
                    newTotalSess
                )

                val updatedProfile = profile.copy(
                    totalFocusedMinutes = newTotalMs,
                    zenLevel = newLevel,
                    zenXP = newLifetimeXp,
                    badges = earnedBadges,
                    totalSessions = newTotalSess,
                    longestEverChain = newLongestEver
                )
                userRepo.saveProfile(updatedProfile)

                withContext(Dispatchers.Main) {
                    _selectedGoal.value = updatedGoal
                    _focusEvents.value = FocusEvent.SessionComplete(minutesFocused, oldChain, updatedGoal.currentChain, xpGain, newBadgesThisSession)
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
}
