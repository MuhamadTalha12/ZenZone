package com.zenzone.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenzone.app.model.UserProfile
import com.zenzone.app.model.ZenBadge
import com.zenzone.app.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepo = UserRepository(application)

    private val _profile = MutableLiveData<UserProfile>()
    val profile: LiveData<UserProfile> = _profile

    private val _badges = MutableLiveData<List<ZenBadge>>()
    val badges: LiveData<List<ZenBadge>> = _badges

    fun loadProfile() {
        viewModelScope.launch {
            val curProfile = userRepo.loadProfile()
            
            val allBadges = listOf(
                ZenBadge("first_session", "First Breath", "Complete your very first focus session.", "ic_badge_first_breath", 0, curProfile.badges.contains("first_session")),
                ZenBadge("chain_3", "Novice Monk", "Reach a 3-day chain.", "ic_badge_chain", 3, curProfile.badges.contains("chain_3")),
                ZenBadge("chain_7", "Week Warrior", "Reach a 7-day chain.", "ic_badge_chain", 7, curProfile.badges.contains("chain_7")),
                ZenBadge("chain_14", "Zen Apprentice", "Reach a 14-day chain.", "ic_badge_chain", 14, curProfile.badges.contains("chain_14")),
                ZenBadge("chain_30", "Digital Monk", "Reach a 30-day chain.", "ic_badge_master", 30, curProfile.badges.contains("chain_30")),
                ZenBadge("chain_60", "Focus Master", "Reach a 60-day chain.", "ic_badge_master", 60, curProfile.badges.contains("chain_60")),
                ZenBadge("chain_100", "Digital Ninja", "Reach a 100-day chain.", "ic_badge_master", 100, curProfile.badges.contains("chain_100")),
                ZenBadge("hours_10", "Time Bender", "Accumulate 10 hours of total focus time.", "ic_badge_time", 0, curProfile.badges.contains("hours_10"))
            )

            withContext(Dispatchers.Main) {
                _profile.value = curProfile
                _badges.value = allBadges
            }
        }
    }
}
