package com.zenzone.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.utils.ChainCalculator
import com.zenzone.app.utils.Constants
import com.zenzone.app.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FocusRepository(application)
    private val _goals = MutableLiveData<List<FocusGoal>>()
    val goals: LiveData<List<FocusGoal>> = _goals

    fun loadGoals() {
        viewModelScope.launch {
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

            withContext(Dispatchers.Main) {
                _goals.value = updatedGoals
            }
        }
    }

    fun addGoal(goal: FocusGoal) {
        viewModelScope.launch {
            val currentList = _goals.value.orEmpty().toMutableList()
            currentList.add(goal)
            repository.saveGoals(currentList)
            withContext(Dispatchers.Main) {
                _goals.value = currentList
            }
        }
    }

    fun updateGoal(goal: FocusGoal) {
        viewModelScope.launch {
            val currentList = _goals.value.orEmpty().toMutableList()
            val idx = currentList.indexOfFirst { it.id == goal.id }
            if (idx != -1) {
                currentList[idx] = goal
                repository.saveGoals(currentList)
                withContext(Dispatchers.Main) {
                    _goals.value = currentList
                }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            val currentList = _goals.value.orEmpty().toMutableList()
            currentList.removeAll { it.id == goalId }
            repository.saveGoals(currentList)
            withContext(Dispatchers.Main) {
                _goals.value = currentList
            }
        }
    }
}
