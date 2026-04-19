package com.zenzone.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository

class HomeViewModelFactory(
    private val focusRepository: FocusRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] 
                ?: throw IllegalArgumentException("Application is missing in CreationExtras")
            return HomeViewModel(application as Application, focusRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        throw UnsupportedOperationException("HomeViewModelFactory requires CreationExtras to access Application")
    }
}
