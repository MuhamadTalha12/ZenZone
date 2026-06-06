package com.zenzone.app.repository

import android.content.Context
import androidx.work.*
import com.zenzone.app.model.UserProfile
import com.zenzone.app.utils.JsonStorageHelper
import com.zenzone.app.utils.SyncWorker

class UserRepository(private val context: Context) {
    suspend fun loadProfile(): UserProfile = JsonStorageHelper.loadProfile(context)
    
    suspend fun saveProfile(profile: UserProfile) {
        JsonStorageHelper.saveProfile(context, profile)
        enqueueSync()
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            "zenzone_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
