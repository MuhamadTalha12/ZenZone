package com.zenzone.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession
import com.zenzone.app.model.UserProfile
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val focusRepo = FocusRepository(application)
    private val userRepo = UserRepository(application)

    private val _sessions = MutableLiveData<List<FocusSession>>()
    val sessions: LiveData<List<FocusSession>> = _sessions

    private val _profile = MutableLiveData<UserProfile>()
    val profile: LiveData<UserProfile> = _profile

    private val _weeklyMinutesMap = MutableLiveData<Map<String, Int>>()
    val weeklyMinutesMap: LiveData<Map<String, Int>> = _weeklyMinutesMap
    
    private val _goals = MutableLiveData<List<FocusGoal>>()
    val goals: LiveData<List<FocusGoal>> = _goals
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadStats() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val allSessions = focusRepo.loadSessions()
                val curProfile = userRepo.loadProfile()
                val allGoals = focusRepo.loadGoals()

                val weeklyMap = mutableMapOf<String, Int>()
                for (s in allSessions) {
                    val dateStr = s.completedAt.substringBefore("T")
                    weeklyMap[dateStr] = (weeklyMap[dateStr] ?: 0) + s.durationMinutes
                }

                withContext(Dispatchers.Main) {
                    _sessions.value = allSessions.sortedByDescending { it.completedAt }
                    _profile.value = curProfile
                    _weeklyMinutesMap.value = weeklyMap
                    _goals.value = allGoals
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to load stats: ${e.message}"
                    _isLoading.value = false
                    // Set default values to prevent crashes
                    if (_sessions.value == null) {
                        _sessions.value = emptyList()
                    }
                    if (_profile.value == null) {
                        _profile.value = UserProfile(
                            userName = "",
                            totalFocusedMinutes = 0,
                            zenLevel = 1,
                            zenXP = 0,
                            badges = emptyList(),
                            totalSessions = 0,
                            longestEverChain = 0
                        )
                    }
                    if (_weeklyMinutesMap.value == null) {
                        _weeklyMinutesMap.value = emptyMap()
                    }
                    if (_goals.value == null) {
                        _goals.value = emptyList()
                    }
                }
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
