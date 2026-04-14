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

    fun loadStats() {
        viewModelScope.launch {
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
            }
        }
    }
}
