package com.zenzone.app.model

data class UserProfile(
    val userName: String = "",
    val profileImageUri: String? = null,
    val totalFocusedMinutes: Long,
    val zenLevel: Int,
    val zenXP: Int,
    val badges: List<String>,
    val totalSessions: Int,
    val currentChain: Int = 0,
    val longestEverChain: Int
)
