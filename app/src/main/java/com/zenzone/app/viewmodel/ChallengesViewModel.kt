package com.zenzone.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenzone.app.model.ChallengeEntity
import com.zenzone.app.repository.ChallengeRepository
import com.zenzone.app.utils.DateUtils
import kotlinx.coroutines.launch

class ChallengesViewModel(application: Application) : AndroidViewModel(application) {
    private val challengeRepo = ChallengeRepository(application)

    private val _challengeCompletedEvent = MutableLiveData<ChallengeEntity?>()
    val challengeCompletedEvent: LiveData<ChallengeEntity?> = _challengeCompletedEvent

    fun getTodaysChallenges(): LiveData<List<ChallengeEntity>> {
        val today = DateUtils.getTodayString()
        
        // Also fetch from Firestore in background if local Room is empty
        viewModelScope.launch {
            val local = challengeRepo.getChallengesForDateSync(today)
            if (local.isEmpty()) {
                val success = challengeRepo.fetchAndCacheChallenges(today)
                if (!success) {
                    challengeRepo.generateAndUploadDefaultChallenges(today)
                }
            }
        }
        
        return challengeRepo.getTodaysChallenges(today)
    }

    fun updateChallengeProgress(type: String, incrementValue: Int) {
        viewModelScope.launch {
            challengeRepo.updateChallengeProgress(type, incrementValue) { completedChallenge ->
                _challengeCompletedEvent.postValue(completedChallenge)
            }
        }
    }

    fun awardXP(xpReward: Int) {
        // Implement awardXP in accordance with instructions
        viewModelScope.launch {
            challengeRepo.updateChallengeProgress("FOCUS_DURATION", xpReward)
        }
    }

    fun clearCompletedEvent() {
        _challengeCompletedEvent.value = null
    }
}
