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
    
    private var timer: CountDownTimer? = null
    
    fun loadGoals() {
        viewModelScope.launch {
            val list = focusRepo.loadGoals()
            withContext(Dispatchers.Main) {
                _goals.value = list
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
        
        timer = object : CountDownTimer(totalMs, 1000) {
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

            val earnedBadges = profile.badges.toMutableList()
            val newBadgesThisSession = mutableListOf<ZenBadge>()
            
            val checkBadge = { id: String, name: String, chainReq: Int, checkVal: Boolean ->
                if (checkVal && !earnedBadges.contains(id)) {
                    earnedBadges.add(id)
                    newBadgesThisSession.add(ZenBadge(id, name, "", "ic_lotus_logo", chainReq, true))
                }
            }
            
            checkBadge("first_session", "First Breath", 0, newTotalSess == 1)
            checkBadge("chain_3", "Novice Monk", 3, updatedGoal.currentChain >= 3)
            checkBadge("chain_7", "Week Warrior", 7, updatedGoal.currentChain >= 7)
            checkBadge("chain_14", "Zen Apprentice", 14, updatedGoal.currentChain >= 14)
            checkBadge("chain_30", "Digital Monk", 30, updatedGoal.currentChain >= 30)
            checkBadge("chain_60", "Focus Master", 60, updatedGoal.currentChain >= 60)
            checkBadge("chain_100", "Digital Ninja", 100, updatedGoal.currentChain >= 100)
            checkBadge("hours_10", "Time Bender", 0, newTotalMs >= 600)

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
        }
    }
}
