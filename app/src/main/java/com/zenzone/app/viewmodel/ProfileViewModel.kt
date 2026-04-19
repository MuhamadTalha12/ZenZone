package com.zenzone.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zenzone.app.model.UserProfile
import com.zenzone.app.model.ZenBadge
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepo = UserRepository(application)
    private val focusRepo = FocusRepository(application)

    private val _profile = MutableLiveData<UserProfile>()
    val profile: LiveData<UserProfile> = _profile

    private val _badges = MutableLiveData<List<ZenBadge>>()
    val badges: LiveData<List<ZenBadge>> = _badges

    private val _totalStreak = MutableLiveData<Int>(0)
    val totalStreak: LiveData<Int> = _totalStreak
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val curProfile = userRepo.loadProfile()
                val goals = focusRepo.loadGoals()
                
                val allBadges = com.zenzone.app.utils.BadgeManager.getAllBadges(curProfile)
                
                // Calculate total streak from all goals
                val sumStreak = goals.sumOf { it.currentChain }
                
                // Update profile with current chain from goals
                val updatedProfile = curProfile.copy(currentChain = sumStreak)

                withContext(Dispatchers.Main) {
                    _profile.value = updatedProfile
                    _badges.value = allBadges
                    _totalStreak.value = sumStreak
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to load profile: ${e.message}"
                    _isLoading.value = false
                    // Set default values to prevent crashes
                    if (_profile.value == null) {
                        _profile.value = UserProfile(
                            userName = "",
                            totalFocusedMinutes = 0,
                            zenLevel = 1,
                            zenXP = 0,
                            badges = emptyList(),
                            totalSessions = 0,
                            longestEverChain = 0,
                            currentChain = 0
                        )
                    }
                    if (_badges.value == null) {
                        _badges.value = emptyList()
                    }
                }
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    fun updateProfile(name: String, imageUri: String?) {
        viewModelScope.launch {
            try {
                val currentProfile = _profile.value ?: return@launch
                val updatedProfile = currentProfile.copy(
                    userName = name,
                    profileImageUri = imageUri
                )
                userRepo.saveProfile(updatedProfile)
                withContext(Dispatchers.Main) {
                    _profile.value = updatedProfile
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to update profile: ${e.message}"
                }
            }
        }
    }
}
