package com.zenzone.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession
import com.zenzone.app.model.UserProfile
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.utils.ChainCalculator
import com.zenzone.app.utils.Constants
import com.zenzone.app.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    application: Application,
    private val repository: FocusRepository,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {
    
    private val _goals = MutableLiveData<List<FocusGoal>>()
    val goals: LiveData<List<FocusGoal>> = _goals
    
    private val _recentSessions = MutableLiveData<List<FocusSession>>()
    val recentSessions: LiveData<List<FocusSession>> = _recentSessions
    
    private val _totalStreak = MutableLiveData<Int>()
    val totalStreak: LiveData<Int> = _totalStreak
    
    private val _focusTime = MutableLiveData<String>()
    val focusTime: LiveData<String> = _focusTime
    
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile
    
    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadGoals() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val loadedGoals = repository.loadGoals()
                val today = DateUtils.getTodayString()
                val prefs = getApplication<Application>().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                val lastOpenedDate = prefs.getString(Constants.PREF_LAST_OPENED_DATE, null)

                val updatedGoals = if (lastOpenedDate != today) {
                    // New day, run chain break checks
                    val modifiedList = loadedGoals.map { goal ->
                        if (ChainCalculator.isChainBroken(goal, today)) {
                            goal.copy(currentChain = 0)
                        } else {
                            goal
                        }
                    }
                    repository.saveGoals(modifiedList)
                    prefs.edit().putString(Constants.PREF_LAST_OPENED_DATE, today).apply()
                    modifiedList
                } else {
                    loadedGoals
                }

                val newProfileChain = updatedGoals.map { it.currentChain }.maxOrNull() ?: 0
                val allSessions = repository.loadSessions()
                val actualTotalMinutes = allSessions.sumOf { it.durationMinutes.toLong() }
                
                val profile = userRepository.loadProfile()
                var profileUpdated = false
                var updatedProfile = profile
                
                if (profile.currentChain != newProfileChain) {
                    updatedProfile = updatedProfile.copy(currentChain = newProfileChain)
                    profileUpdated = true
                }
                
                if (profile.totalFocusedMinutes != actualTotalMinutes) {
                    updatedProfile = updatedProfile.copy(totalFocusedMinutes = actualTotalMinutes)
                    profileUpdated = true
                }
                
                if (profileUpdated) {
                    userRepository.saveProfile(updatedProfile)
                }

                withContext(Dispatchers.Main) {
                    _goals.value = updatedGoals
                    calculateTotalStreak(updatedGoals)
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
    
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profile = userRepository.loadProfile()
                withContext(Dispatchers.Main) {
                    _userProfile.value = profile
                    calculateFocusTime(profile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to load profile: ${e.message}"
                }
            }
        }
    }
    
    fun loadRecentSessions() {
        viewModelScope.launch {
            try {
                val allSessions = repository.loadSessions()
                // Get the 5 most recent sessions
                val recent = allSessions.sortedByDescending { it.completedAt }.take(5)
                withContext(Dispatchers.Main) {
                    _recentSessions.value = recent
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to load sessions: ${e.message}"
                }
            }
        }
    }
    
    private fun calculateTotalStreak(goals: List<FocusGoal>) {
        // Sum up all current chains from all goals
        val total = goals.map { it.currentChain }.maxOrNull() ?: 0
        _totalStreak.value = total
    }
    
    private fun calculateFocusTime(profile: UserProfile) {
        val totalMinutes = profile.totalFocusedMinutes
        val hours = totalMinutes / 60.0
        _focusTime.value = if (hours >= 1) {
            String.format("%.1fh", hours)
        } else {
            "${totalMinutes}m"
        }
    }

    fun addGoal(goal: FocusGoal) {
        // Validate goal before adding
        if (goal.name.isBlank()) {
            _validationError.value = "Goal name cannot be empty"
            return
        }
        if (goal.name.length > Constants.MAX_GOAL_NAME_LENGTH) {
            _validationError.value = "Goal name cannot exceed ${Constants.MAX_GOAL_NAME_LENGTH} characters"
            return
        }
        if (goal.targetMinutes <= 0) {
            _validationError.value = "Target minutes must be greater than 0"
            return
        }
        if (goal.targetMinutes > Constants.MAX_TARGET_MINUTES) {
            _validationError.value = "Target minutes cannot exceed ${Constants.MAX_TARGET_MINUTES} minutes"
            return
        }
        
        viewModelScope.launch {
            try {
                val freshList = repository.loadGoals().toMutableList()
                val existingIndex = freshList.indexOfFirst { it.name.equals(goal.name, ignoreCase = true) }
                
                val updatedList = if (existingIndex != -1) {
                    val existing = freshList[existingIndex]
                    val merged = existing.copy(
                        targetMinutes = goal.targetMinutes,
                        frequency = goal.frequency,
                        isSynced = false
                    )
                    freshList[existingIndex] = merged
                    freshList
                } else {
                    freshList.add(goal)
                    freshList
                }

                repository.saveGoals(updatedList)

                val newProfileChain = updatedList.map { it.currentChain }.maxOrNull() ?: 0
                val profile = userRepository.loadProfile()
                if (profile.currentChain != newProfileChain) {
                    val updatedProfile = profile.copy(currentChain = newProfileChain)
                    userRepository.saveProfile(updatedProfile)
                }

                withContext(Dispatchers.Main) {
                    _goals.value = updatedList
                    calculateTotalStreak(updatedList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to add goal: ${e.message}"
                }
            }
        }
    }
    
    fun clearValidationError() {
        _validationError.value = null
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun updateGoal(goal: FocusGoal) {
        viewModelScope.launch {
            try {
                val freshList = repository.loadGoals().toMutableList()
                val idx = freshList.indexOfFirst { it.id == goal.id }
                if (idx != -1) {
                    freshList[idx] = goal
                    repository.saveGoals(freshList)

                    val newProfileChain = freshList.map { it.currentChain }.maxOrNull() ?: 0
                    val profile = userRepository.loadProfile()
                    if (profile.currentChain != newProfileChain) {
                        val updatedProfile = profile.copy(currentChain = newProfileChain)
                        userRepository.saveProfile(updatedProfile)
                    }

                    withContext(Dispatchers.Main) {
                        _goals.value = freshList
                        calculateTotalStreak(freshList)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to update goal: ${e.message}"
                }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            try {
                val freshList = repository.loadGoals().toMutableList()
                freshList.removeAll { it.id == goalId }
                repository.saveGoals(freshList)
                repository.deleteGoalFromFirestore(goalId)

                val newProfileChain = freshList.map { it.currentChain }.maxOrNull() ?: 0
                val profile = userRepository.loadProfile()
                if (profile.currentChain != newProfileChain) {
                    val updatedProfile = profile.copy(currentChain = newProfileChain)
                    userRepository.saveProfile(updatedProfile)
                }

                withContext(Dispatchers.Main) {
                    _goals.value = freshList
                    calculateTotalStreak(freshList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to delete goal: ${e.message}"
                }
            }
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                userRepository.saveProfile(profile)
                withContext(Dispatchers.Main) {
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to update profile: ${e.message}"
                }
            }
        }
    }
}
