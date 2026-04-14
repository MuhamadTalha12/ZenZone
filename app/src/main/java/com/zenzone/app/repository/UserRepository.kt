package com.zenzone.app.repository

import android.content.Context
import com.zenzone.app.model.UserProfile
import com.zenzone.app.utils.JsonStorageHelper

class UserRepository(private val context: Context) {
    suspend fun loadProfile(): UserProfile = JsonStorageHelper.loadProfile(context)
    suspend fun saveProfile(profile: UserProfile) = JsonStorageHelper.saveProfile(context, profile)
}
